export function bootstrapAmapSecurity() {
  const securityJsCode = (import.meta.env.VITE_AMAP_SECURITY_JS_CODE || '').trim()
  if (!securityJsCode) return
  const w = window as Window & { _AMapSecurityConfig?: { securityJsCode: string } }
  w._AMapSecurityConfig = { securityJsCode }
}

