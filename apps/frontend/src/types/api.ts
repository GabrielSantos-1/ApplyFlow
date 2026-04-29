export type UserRole = "USER" | "ADMIN";

export type VacancyStatus = "DRAFT" | "PUBLISHED" | "CLOSED" | "ARCHIVED";

export type ApplicationStatus =
  | "DRAFT"
  | "READY_FOR_REVIEW"
  | "APPLIED"
  | "INTERVIEW"
  | "REJECTED"
  | "OFFER"
  | "WITHDRAWN";

export type TrackingStage = "CREATED" | "SCREENING" | "SUBMITTED" | "INTERVIEW" | "FINAL" | "CLOSED";

export type Recommendation = "APPLY" | "REVIEW" | "IGNORE";
export type MatchState = "GENERATED" | "MISSING_RESUME" | "MISSING_VARIANT" | "NOT_GENERATED";

export type PageResponse<T> = {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type AuthTokensResponse = {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  userId: string;
  email: string;
  role: UserRole;
  refreshToken: string | null;
};

export type CurrentUserResponse = {
  id: string;
  email: string;
  role: UserRole;
};

export type VacancyListItem = {
  id: string;
  title: string;
  company: string;
  location: string;
  remote: boolean;
  seniority: string | null;
  jobUrl: string | null;
  publishedAt: string | null;
  status: VacancyStatus;
  requiredSkills: string[];
  qualityScore?: number | null;
};

export type VacancyDetail = VacancyListItem;

export type MatchAnalysisResponse = {
  vacancyId: string;
  score: number | null;
  recommendation: Recommendation | null;
  scoreBreakdown: Record<string, number>;
  strengths: string[];
  gaps: string[];
  keywordsToAdd: string[];
  state: MatchState;
  algorithmVersion: string;
  generatedAt: string | null;
  hasResumeContext: boolean;
  hasVariantContext: boolean;
};

export type MatchEnrichmentResponse = {
  vacancyId: string;
  deterministicScore: number;
  deterministicRecommendation: Recommendation;
  deterministicBreakdown: Record<string, number>;
  summary: string;
  strengths: string[];
  gaps: string[];
  nextSteps: string[];
  fallbackUsed: boolean;
};

export type CvImprovementResponse = {
  vacancyId: string;
  deterministicScore: number;
  deterministicRecommendation: Recommendation;
  deterministicBreakdown: Record<string, number>;
  improvementSuggestions: string[];
  atsKeywords: string[];
  highlightPoints: string[];
  gapActions: string[];
  fallbackUsed: boolean;
};

export type ApplicationDraftSuggestionResponse = {
  vacancyId: string;
  deterministicScore: number;
  deterministicRecommendation: Recommendation;
  deterministicBreakdown: Record<string, number>;
  shortMessage: string;
  miniCoverNote: string;
  fallbackUsed: boolean;
};

export type ResumeResponse = {
  id: string;
  title: string;
  sourceFileName: string;
  status: "ACTIVE" | "ARCHIVED";
  base: boolean;
  contentType: string | null;
  fileSizeBytes: number | null;
  fileChecksumSha256: string | null;
  uploadedAt: string | null;
};

export type ResumeVariantResponse = {
  id: string;
  resumeId: string;
  vacancyId: string;
  variantLabel: string;
  status: "DRAFT" | "READY_FOR_REVIEW" | "APPROVED";
};

export type CreateResumeMetadataRequest = {
  title: string;
  sourceFileName: string;
};

export type UploadResumePdfRequest = {
  title: string;
  file: File;
};

export type CreateResumeVariantRequest = {
  vacancyId: string;
  variantLabel?: string;
};

export type ApplicationDraftResponse = {
  id: string;
  vacancyId: string;
  resumeVariantId: string;
  status: ApplicationStatus;
  messageDraft: string;
};

export type ApplicationTrackingEventResponse = {
  id: string;
  stage: TrackingStage;
  notes: string | null;
  createdAt: string;
};

export type AdminIngestionOverviewResponse = {
  providers: AdminProviderOverview[];
  totals: AdminTotalsOverview;
  quality: AdminQualityOverview;
  dedupe: AdminDedupeOverview;
  recent: AdminRecentOverview;
};

export type AdminProviderOverview = {
  sourceConfigId: string;
  name: string;
  sourceType: string;
  tenant: string | null;
  active: boolean;
  vacanciesCollected: number;
  vacanciesPersisted: number;
  duplicateVacancies: number;
  averageQualityScore: number;
  lastExecution: AdminLastExecutionOverview | null;
};

export type AdminLastExecutionOverview = {
  status: string;
  durationMs: number;
  startedAt: string | null;
  finishedAt: string | null;
  fetchedCount: number;
  persistedCount: number;
  skippedCount: number;
  failedCount: number;
};

export type AdminTotalsOverview = {
  providers: number;
  activeProviders: number;
  vacanciesCollected: number;
  vacanciesPersisted: number;
  vacanciesTotal: number;
  vacanciesVisible: number;
};

export type AdminQualityOverview = {
  averageQualityScore: number;
  topFlags: AdminQualityFlagOverview[];
};

export type AdminQualityFlagOverview = {
  flag: string;
  count: number;
};

export type AdminDedupeOverview = {
  totalVacancies: number;
  duplicateVacancies: number;
  duplicateRatePercent: number;
};

export type AdminRecentOverview = {
  last24h: number;
  last7d: number;
};

export type ApiErrorBody = {
  error?: string;
  errorCode?: string;
  message?: string;
  path?: string;
  status?: number;
  timestamp?: string;
};

