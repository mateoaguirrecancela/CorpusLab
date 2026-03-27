import { type CountryOption } from '@/modules/auth/types/signup'

type IntlWithSupportedValues = {
  supportedValuesOf?: (key: 'calendar' | 'collation' | 'currency' | 'numberingSystem' | 'timeZone' | 'unit' | 'region') => string[]
}

const FALLBACK_COUNTRY_CODES = [
  'AD', 'AE', 'AF', 'AG', 'AL', 'AM', 'AO', 'AR', 'AT', 'AU', 'AZ', 'BA', 'BB', 'BD', 'BE', 'BF', 'BG', 'BH',
  'BI', 'BJ', 'BN', 'BO', 'BR', 'BS', 'BT', 'BW', 'BY', 'BZ', 'CA', 'CD', 'CF', 'CG', 'CH', 'CI', 'CL', 'CM',
  'CN', 'CO', 'CR', 'CU', 'CV', 'CY', 'CZ', 'DE', 'DJ', 'DK', 'DO', 'DZ', 'EC', 'EE', 'EG', 'ER', 'ES', 'ET',
  'FI', 'FJ', 'FM', 'FR', 'GA', 'GB', 'GD', 'GE', 'GH', 'GM', 'GN', 'GQ', 'GR', 'GT', 'GW', 'GY', 'HN', 'HR',
  'HT', 'HU', 'ID', 'IE', 'IL', 'IN', 'IQ', 'IR', 'IS', 'IT', 'JM', 'JO', 'JP', 'KE', 'KG', 'KH', 'KI', 'KM',
  'KN', 'KP', 'KR', 'KW', 'KZ', 'LA', 'LB', 'LC', 'LI', 'LK', 'LR', 'LS', 'LT', 'LU', 'LV', 'LY', 'MA', 'MC',
  'MD', 'ME', 'MG', 'MH', 'MK', 'ML', 'MM', 'MN', 'MR', 'MT', 'MU', 'MV', 'MW', 'MX', 'MY', 'MZ', 'NA', 'NE',
  'NG', 'NI', 'NL', 'NO', 'NP', 'NR', 'NZ', 'OM', 'PA', 'PE', 'PG', 'PH', 'PK', 'PL', 'PT', 'PW', 'PY', 'QA',
  'RO', 'RS', 'RU', 'RW', 'SA', 'SB', 'SC', 'SD', 'SE', 'SG', 'SI', 'SK', 'SL', 'SM', 'SN', 'SO', 'SR', 'SS',
  'ST', 'SV', 'SY', 'SZ', 'TD', 'TG', 'TH', 'TJ', 'TL', 'TM', 'TN', 'TO', 'TR', 'TT', 'TV', 'TW', 'TZ', 'UA',
  'UG', 'US', 'UY', 'UZ', 'VA', 'VC', 'VE', 'VN', 'VU', 'WS', 'YE', 'ZA', 'ZM', 'ZW',
]

function getRegionCodes(): string[] {
  const intlWithSupportedValues = Intl as unknown as IntlWithSupportedValues

  if (typeof intlWithSupportedValues.supportedValuesOf === 'function') {
    try {
      const regionCodes = intlWithSupportedValues.supportedValuesOf('region')
      if (Array.isArray(regionCodes) && regionCodes.length > 0) {
        return regionCodes
      }
    } catch {
      return FALLBACK_COUNTRY_CODES
    }
  }

  return FALLBACK_COUNTRY_CODES
}

export function buildCountryOptions(locale = 'en'): CountryOption[] {
  const displayNames = typeof Intl.DisplayNames === 'function'
    ? new Intl.DisplayNames([locale], { type: 'region' })
    : null

  return getRegionCodes()
    .filter((code: string) => /^[A-Z]{2}$/.test(code))
    .map((code: string): CountryOption => ({ value: code, label: displayNames?.of(code) ?? code }))
    .sort((a: CountryOption, b: CountryOption) => a.label.localeCompare(b.label))
}

const COUNTRY_LABELS_BY_CODE = new Map(buildCountryOptions('es').map((option) => [option.value, option.label]))

export function getCountryLabelByCode(countryCode: string | null | undefined): string {
  if (!countryCode) {
    return '-'
  }

  const normalizedCode = countryCode.trim().toUpperCase()
  if (normalizedCode.length === 0) {
    return '-'
  }

  return COUNTRY_LABELS_BY_CODE.get(normalizedCode) ?? normalizedCode
}
