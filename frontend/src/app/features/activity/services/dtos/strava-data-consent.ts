export interface StravaDataConsentStatus {
  valid: boolean;
  currentConsentVersion: string;
  purpose: 'STRAVA_DATA_IMPORT_AND_COACHING';
}

export interface StartStravaLinkingRequest {
  acceptedStravaDataConsent: boolean;
  language: string;
}
