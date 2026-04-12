import rawAreaData from 'china-area-data/data.json'

export type CascaderOption = {
  value: string
  label: string
  children?: CascaderOption[]
}

/**
 * 生成“省/市”二级联动数据（不含区县）。
 * pcaa: { [provinceCode]: provinceName, [cityCode]: cityName, ... } 结构为 { 86:省, 省code:市, 市code:区 }
 */
export function buildProvinceCityOptions(): CascaderOption[] {
  const areaData = rawAreaData as Record<string, Record<string, string>>

  const provinces = (areaData['86'] || {}) as Record<string, string>
  const excludedProvinceCodes = new Set(['710000', '810000', '820000']) // 台/港/澳
  const municipalities = new Set(['110000', '120000', '310000', '500000']) // 京/津/沪/渝

  return Object.keys(provinces)
    .filter((provinceCode) => !excludedProvinceCodes.has(provinceCode))
    .map((provinceCode) => {
    const provinceName = provinces[provinceCode]
    const level2 = (areaData[provinceCode] || {}) as Record<string, string>

    // 直辖市：不下钻到区县，市级就是它本身
    if (municipalities.has(provinceCode)) {
      return {
        value: provinceCode,
        label: provinceName,
        children: [{ value: provinceCode, label: provinceName }]
      }
    }

    // 其他省份：只到“市级”，过滤掉占位节点（不下钻到区县）
    const cityOptions: CascaderOption[] = Object.keys(level2)
      .map((cityCode) => ({ value: cityCode, label: level2[cityCode] }))
      .filter((c) => {
        const n = c.label
        if (!n) return false
        if (n === '市辖区' || n === '县') return false
        if (n === '省直辖县级行政区划') return false
        return true
      })

    return {
      value: provinceCode,
      label: provinceName,
      children: cityOptions
    }
  })
}

