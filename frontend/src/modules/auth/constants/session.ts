export const SESSION_USER_STORAGE_KEY = 'corpuslab.sessionUser'
export const SESSION_AUTH_TOKEN_STORAGE_KEY = 'corpuslab.authToken'

export type OAuthProvider = 'google' | 'github'

export const OAUTH_PROVIDER_LABEL: Record<OAuthProvider, string> = {
	google: 'Google',
	github: 'GitHub',
}
