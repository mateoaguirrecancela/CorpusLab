import githubLogo from '@/assets/github.svg';
import googleLogo from '@/assets/google.svg';
import { Button } from '@/components/ui/button';
import { type OAuthProvider } from '@/modules/auth/constants/session';

type OAuthButtonsProps = {
  disabled: boolean;
  onProviderClick: (provider: OAuthProvider) => void;
};

const OAUTH_PROVIDERS: readonly {
  label: string;
  logo: string;
  provider: OAuthProvider;
}[] = [
  { label: 'Google', logo: googleLogo, provider: 'google' },
  { label: 'GitHub', logo: githubLogo, provider: 'github' },
];

export function OAuthButtons({ disabled, onProviderClick }: Readonly<OAuthButtonsProps>) {
  return (
    <div className="grid gap-3 sm:grid-cols-2">
      {OAUTH_PROVIDERS.map(({ label, logo, provider }) => (
        <Button
          className="h-10 cursor-pointer rounded-md border border-border bg-surface-base text-sm font-semibold text-foreground hover:bg-accent"
          disabled={disabled}
          key={provider}
          onClick={() => onProviderClick(provider)}
          type="button"
          variant="outline"
        >
          <img alt="" aria-hidden className="size-4" src={logo} />
          <span>{label}</span>
        </Button>
      ))}
    </div>
  );
}
