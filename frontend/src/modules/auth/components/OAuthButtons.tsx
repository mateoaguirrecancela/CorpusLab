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
          disabled={disabled}
          key={provider}
          onClick={() => onProviderClick(provider)}
          size="action"
          type="button"
          variant="secondaryAction"
        >
          <img alt="" aria-hidden className="size-4" src={logo} />
          <span>{label}</span>
        </Button>
      ))}
    </div>
  );
}
