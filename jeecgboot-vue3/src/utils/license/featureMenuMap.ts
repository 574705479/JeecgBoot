export const FEATURE_MENU_MAP: Record<string, string[]> = {
  'airag': ['/airag/'],
  'ai_knowledge': ['/airag/knowledge'],
  'ai_ocr': ['/airag/ocr'],
  'cs': ['/cs/'],
  'cs_security': ['/cs/security'],
  'lowcode': ['/online/'],
  'report': ['/report', '/chart'],
  'bigscreen': ['/bigscreen'],
  'dashboard_design': ['/dashboard/design'],
  'workflow': ['/workflow'],
  'openapi': ['/openapi'],
  'monitor': ['/monitor'],
  'tenant': ['/system/tenant'],
};

/**
 * Check if a menu path is allowed by the licensed features.
 * Uses longest-prefix-match: more specific prefixes (e.g. /cs/security)
 * take priority over broader ones (e.g. /cs/).
 * Paths not matching any prefix are allowed by default.
 */
export function isMenuAllowed(menuPath: string, features: string[] | null | undefined): boolean {
  if (!features || features.length === 0) return true;

  let bestMatchLen = 0;
  let bestFeature = '';

  for (const [feature, prefixes] of Object.entries(FEATURE_MENU_MAP)) {
    for (const prefix of prefixes) {
      if (menuPath.startsWith(prefix) && prefix.length > bestMatchLen) {
        bestMatchLen = prefix.length;
        bestFeature = feature;
      }
    }
  }

  if (bestFeature) {
    return features.includes(bestFeature);
  }
  return true;
}
