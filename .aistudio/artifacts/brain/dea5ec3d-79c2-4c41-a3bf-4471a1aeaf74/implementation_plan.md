# OmniSync: Document & Mail Manager with Gemini AI

OmniSync is an intelligent document and attachment workspace that connects directly to Google Drive, Gmail, and the Gemini API—allowing users to seamlessly manage files, bridge email attachments, compose messages with Drive documents, and generate instant AI summaries.

### User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were aligned based on your responses and will guide the full implementation:

- **Confirmed Workflow**: Document and attachment manager bridging Google Drive and Gmail.
- **Confirmed API Integration**: Gemini API (`gemini-3.5-flash`) for summarizing emails, extracting attachment details, and drafting emails.
- **Confirmed Access Scope**: Full access to Google Drive (file search, view, upload) and Gmail (thread viewing, attachment downloading, message drafting, and sending).
- **Workspace Authentication**: When approved, we will trigger the built-in `set_up_oauth` flow for Google Drive and Gmail scopes, while Gemini API is securely injected via AI Studio Secrets and `BuildConfig.GEMINI_API_KEY`.

---

### 1. Overview & Core Concept

- **What It Does**: OmniSync unites Google Drive and Gmail into a single, cohesive Android dashboard. Users can inspect Drive files, discover and organize attachments across email threads, attach Drive files directly into outgoing emails, and run Gemini AI to summarize complex threads or file contents into concise bullet points and action items.
- **Target Audience / Persona**: Professionals, students, and power users who deal with heavy file workflows across email and cloud storage and need quick summarization and attachment management on Android.
- **Key Value**: Eliminates context-switching between Drive and Gmail apps, provides one-click AI analysis of email attachments, and keeps a cached local offline record of recent items.

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **Google Services Connection & Status Hub**:
   - Clean connection banner showing Google Account status (Drive, Gmail, Gemini API).
   - Instant sync status indicator and quick actions (Sync Now, Compose Email, Upload File).
2. **Unified Document & Attachment Explorer**:
   - Filterable view switching between **Drive Files**, **Mail Attachments**, and **Recent Summaries**.
   - Search bar with instant filtering by filename, sender, or file type (PDF, Docs, Sheets, Images).
   - Detail Bottom Sheet showing file metadata, preview information, one-tap "Summarize with Gemini", and "Attach to Email".
3. **Smart Email Composer with Drive Attachment Bridge**:
   - Recipient, subject, and body fields with formatting.
   - "Attach from Drive" drawer allowing users to pick files directly from their Drive folder without downloading first.
   - "AI Draft / Polish" button powered by Gemini to refine email tone or generate replies from bullet points.
4. **Gemini AI Summary & Action Items Pane**:
   - Dedicated analysis sheet showing key highlights, executive summary, extracted dates/deadlines, and generated next actions.
   - History of generated summaries saved to Room for instant offline access.

#### Visual Identity & Theme
- **Aesthetic Direction**: *Executive Tech / Modern Productivity*—crisp, authoritative, uncluttered layout with subtle elevation and high-contrast typography.
- **Color Palette**:
  - Primary: Deep Cobalt Blue (`#1A56DB` / Dark: `#6895F7`)
  - Secondary / Accent: Electric Cyan (`#06B6D4`) & Emerald (`#10B981` for active sync states)
  - Surface & Background: Light slate tinted canvas (`#F8FAFC` / Dark: `#0F172A`)
  - Elevated Cards: Pure white (`#FFFFFF` / Dark: `#1E293B`) with 1dp border tones (`#E2E8F0` / Dark: `#334155`)
- **Typography & Hierarchy**:
  - Bold, legible headlines with strong typographic scale (`TitleMedium`, `HeadlineSmall`).
  - Clear metadata badges for file size, MIME types, date timestamps, and email sender tags.
- **Interactive Feedback & Motion**:
  - Spring-animated tab transitions and expandable cards (`AnimatedVisibility`).
  - Haptic feedback and Material ripples on all card actions and buttons.
  - Shimmer placeholders during Drive / Gmail / Gemini network operations.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Google Authentication & API Integration Strategy**
  - *Chosen Approach*: Use AI Studio's native `set_up_oauth` mechanism for Drive and Gmail scopes, paired with Android Credential Manager / Google Workspace REST APIs with token injection. For Gemini, use the direct REST API with `gemini-3.5-flash` using `BuildConfig.GEMINI_API_KEY`.
  - *Why*: Provides real Google cloud connectivity without mock data, compliant with AI Studio security standards and Android runtime policies.
  - *Alternatives Considered*: Firebase Auth only (lacks native Drive/Gmail Workspace API delegation without extra OAuth configuration).

- **Decision 2: Local Persistence & Offline Caching**
  - *Chosen Approach*: Android Room Database storing cached Drive file metadata, email threads, attachment indexes, and generated AI summaries.
  - *Why*: Guarantees instant app launch, offline browsing of previously viewed files/emails, and saves Gemini API quota by caching generated summaries.

- **Decision 3: Zero-Permission Media & File Management**
  - *Chosen Approach*: Use Android Photo Picker and Storage Access Framework (`ActivityResultContracts.OpenDocument` / `PickVisualMedia`) for local file uploads, avoiding legacy storage permissions.
  - *Why*: 100% compliant with Google Play policy and user privacy standards.

---

### 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────────┐
│                       OmniSync UI Layer                         │
├─────────────────┬───────────────────┬───────────────────────────┤
│  Explorer Tab   │   Composer Tab    │     AI Summary Modal      │
│ (Drive + Mail)  │(Drive Attachments)│    (Threads & Files)      │
└────────┬────────┴─────────┬─────────┴─────────────┬─────────────┘
         │                  │                       │
         ▼                  ▼                       ▼
┌─────────────────────────────────────────────────────────────────┐
│               OmniSyncViewModel (State & Flow)                  │
├─────────────────────────────────────────────────────────────────┤
│ - UI State (DriveFiles, MailThreads, Attachments, Summaries)    │
│ - Filter & Search state, Sync status, Composer form state       │
└────────┬──────────────────┬───────────────────────┬─────────────┘
         │                  │                       │
         ▼                  ▼                       ▼
┌──────────────────┐┌──────────────────┐┌────────────────────────┐
│ WorkspaceService ││  GeminiService   ││    Room Local DB       │
│ - Drive API v3   ││  - Gemini-3.5    ││ - Cached Files Table   │
│ - Gmail API v1   ││  - Summarizer    ││ - Email Threads Table  │
│ - OAuth Token    ││  - Draft Polisher││ - AI Summaries Table   │
└──────────────────┘└──────────────────┘└────────────────────────┘
```

#### Data Entities & Room Tables
- `DriveFileEntity`: ID, title, mimeType, webViewLink, sizeBytes, modifiedTime, thumbnailLink.
- `EmailThreadEntity`: ID, threadId, sender, recipient, subject, snippet, timestamp, hasAttachments.
- `AttachmentEntity`: ID, messageId, filename, mimeType, sizeBytes, driveFileId, isCached.
- `AiSummaryEntity`: ID, sourceId, sourceType (`FILE` or `EMAIL`), title, summaryText, keyActionItems, timestamp.

#### Interactive Component & State Mapping
- **Tab Navigation**: Seamless switching between "Files & Attachments", "Mail Inbox", and "Saved AI Insights".
- **Drive Attachment Picker**: Bottom sheet listing Drive files with single-tap attach checkbox feeding into the Email Composer state.
- **One-Tap AI Summarize**: Button on any email or document that triggers background Gemini coroutine call, updates loading spinner, and renders structured bullet points.
- **Email Sending Flow**: Pre-validates recipient and message body, builds multipart MIME payload with Drive links/attachments, calls Gmail API `send`, and persists sent copy.

---

### 5. Implementation Stages & Build Steps

1. **Step 1: Application Identity & Platform Sync**
   - Configure `metadata.json` (`name = "OmniSync"`, description).
   - Update `app_name` in `res/values/strings.xml`.
   - Uncomment `GEMINI_API_KEY` in `.env.example`.
2. **Step 2: Dependencies & Permissions**
   - Add required libraries in `app/build.gradle.kts` (Retrofit kotlinx serialization, Room KSP, navigation/material icons).
   - Declare `INTERNET` and `ACCESS_NETWORK_STATE` in `AndroidManifest.xml`.
3. **Step 3: Database & Models**
   - Implement Room entities, DAOs, and `OmniSyncDatabase`.
   - Create domain models for Drive files, Email threads, and AI summaries.
4. **Step 4: Networking & Services**
   - Build `GeminiApiService` using `gemini-3.5-flash` with structured generation and timeouts.
   - Build `WorkspaceApiService` for Drive and Gmail operations.
   - Setup OAuth configuration tool call (`set_up_oauth`).
5. **Step 5: Jetpack Compose UI**
   - Theme tokens, typography, and color palette.
   - Top Bar with sync indicator and account status.
   - Document & Attachment Explorer with search and filter chips.
   - Email Composer with Drive Attachment bridge.
   - Bottom sheet for file details and Gemini AI summaries.
6. **Step 6: Verification & Compilation**
   - Run `compile_applet` to ensure successful compilation and zero errors.
