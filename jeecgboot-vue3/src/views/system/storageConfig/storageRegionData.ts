/**
 * 存储配置「地域」可视化：分组 + 中文名 + 技术标识（与控制台习惯一致）
 * - 腾讯云：value 为 COS Region（ap-*）
 * - 阿里云：value 为 OSS Endpoint 域名（oss-cn-*.aliyuncs.com）
 */

export interface RegionItem {
  /** 提交给后端的值 */
  value: string;
  /** 下拉展示：名称 (技术标识) */
  label: string;
}

export interface RegionGroup {
  group: string;
  options: RegionItem[];
}

/** 腾讯云 COS */
export const TENCENT_REGION_GROUPS: RegionGroup[] = [
  {
    group: '中国大陆公有云地域',
    options: [
      { value: 'ap-beijing', label: '北京 (ap-beijing)' },
      { value: 'ap-nanjing', label: '南京 (ap-nanjing)' },
      { value: 'ap-shanghai', label: '上海 (ap-shanghai)' },
      { value: 'ap-guangzhou', label: '广州 (ap-guangzhou)' },
      { value: 'ap-shenzhen', label: '深圳 (ap-shenzhen)' },
      { value: 'ap-chengdu', label: '成都 (ap-chengdu)' },
      { value: 'ap-chongqing', label: '重庆 (ap-chongqing)' },
      { value: 'ap-qingyuan', label: '清远 (ap-qingyuan)' },
      { value: 'ap-heyuan', label: '河源 (ap-heyuan)' },
      { value: 'ap-guiyang', label: '贵阳 (ap-guiyang)' },
      { value: 'ap-wuhan', label: '武汉 (ap-wuhan)' },
      { value: 'ap-xian', label: '西安 (ap-xian)' },
      { value: 'ap-jinan', label: '济南 (ap-jinan)' },
      { value: 'ap-shijiazhuang', label: '石家庄 (ap-shijiazhuang)' },
      { value: 'ap-taiyuan', label: '太原 (ap-taiyuan)' },
      { value: 'ap-shenyang', label: '沈阳 (ap-shenyang)' },
    ],
  },
  {
    group: '中国大陆金融云地域',
    options: [
      { value: 'ap-shenzhen-fsi', label: '深圳金融 (ap-shenzhen-fsi)' },
      { value: 'ap-shanghai-fsi', label: '上海金融 (ap-shanghai-fsi)' },
      { value: 'ap-beijing-fsi', label: '北京金融 (ap-beijing-fsi)' },
    ],
  },
  {
    group: '港澳台及境外',
    options: [
      { value: 'ap-hongkong', label: '中国香港 (ap-hongkong)' },
      { value: 'ap-singapore', label: '新加坡 (ap-singapore)' },
      { value: 'ap-mumbai', label: '孟买 (ap-mumbai)' },
      { value: 'ap-seoul', label: '首尔 (ap-seoul)' },
      { value: 'ap-tokyo', label: '东京 (ap-tokyo)' },
      { value: 'na-siliconvalley', label: '硅谷 (na-siliconvalley)' },
      { value: 'na-ashburn', label: '弗吉尼亚 (na-ashburn)' },
      { value: 'eu-frankfurt', label: '法兰克福 (eu-frankfurt)' },
    ],
  },
];

/** 阿里云 OSS 公共 Endpoint（控制台常见地域，不含内网/双栈变种） */
export const ALIYUN_REGION_GROUPS: RegionGroup[] = [
  {
    group: '中国大陆公有云地域',
    options: [
      { value: 'oss-cn-hangzhou.aliyuncs.com', label: '华东1（杭州） (oss-cn-hangzhou)' },
      { value: 'oss-cn-shanghai.aliyuncs.com', label: '华东2（上海） (oss-cn-shanghai)' },
      { value: 'oss-cn-nanjing.aliyuncs.com', label: '华东5（南京） (oss-cn-nanjing)' },
      { value: 'oss-cn-fuzhou.aliyuncs.com', label: '华东6（福州） (oss-cn-fuzhou)' },
      { value: 'oss-cn-wuhan.aliyuncs.com', label: '华中1（武汉） (oss-cn-wuhan)' },
      { value: 'oss-cn-qingdao.aliyuncs.com', label: '华北1（青岛） (oss-cn-qingdao)' },
      { value: 'oss-cn-beijing.aliyuncs.com', label: '华北2（北京） (oss-cn-beijing)' },
      { value: 'oss-cn-zhangjiakou.aliyuncs.com', label: '华北3（张家口） (oss-cn-zhangjiakou)' },
      { value: 'oss-cn-huhehaote.aliyuncs.com', label: '华北5（呼和浩特） (oss-cn-huhehaote)' },
      { value: 'oss-cn-wulanchabu.aliyuncs.com', label: '华北6（乌兰察布） (oss-cn-wulanchabu)' },
      { value: 'oss-cn-shenzhen.aliyuncs.com', label: '华南1（深圳） (oss-cn-shenzhen)' },
      { value: 'oss-cn-heyuan.aliyuncs.com', label: '华南2（河源） (oss-cn-heyuan)' },
      { value: 'oss-cn-guangzhou.aliyuncs.com', label: '华南3（广州） (oss-cn-guangzhou)' },
      { value: 'oss-cn-chengdu.aliyuncs.com', label: '西南1（成都） (oss-cn-chengdu)' },
    ],
  },
  {
    group: '中国大陆金融云地域',
    options: [
      { value: 'oss-cn-hangzhou-finance-1.aliyuncs.com', label: '杭州金融1 (oss-cn-hangzhou-finance-1)' },
      { value: 'oss-cn-shanghai-finance-1.aliyuncs.com', label: '上海金融1 (oss-cn-shanghai-finance-1)' },
      { value: 'oss-cn-shenzhen-finance-1.aliyuncs.com', label: '深圳金融1 (oss-cn-shenzhen-finance-1)' },
      { value: 'oss-cn-beijing-finance-1.aliyuncs.com', label: '北京金融1 (oss-cn-beijing-finance-1)' },
    ],
  },
  {
    group: '港澳台及境外',
    options: [
      { value: 'oss-cn-hongkong.aliyuncs.com', label: '中国香港 (oss-cn-hongkong)' },
      { value: 'oss-ap-southeast-1.aliyuncs.com', label: '新加坡 (oss-ap-southeast-1)' },
      { value: 'oss-ap-northeast-1.aliyuncs.com', label: '日本东京 (oss-ap-northeast-1)' },
      { value: 'oss-us-west-1.aliyuncs.com', label: '美国硅谷 (oss-us-west-1)' },
      { value: 'oss-eu-central-1.aliyuncs.com', label: '德国法兰克福 (oss-eu-central-1)' },
    ],
  },
];

export function flattenRegionOptions(groups: RegionGroup[]): RegionItem[] {
  return groups.flatMap((g) => g.options);
}

/** 若后端已有值不在内置列表（历史数据/专有云），顶部补一条便于回显与保存 */
/** 与内置列表忽略大小写对齐，便于回显选中项 */
export function normalizeAliyunEndpoint(ep: string | undefined): string {
  const t = (ep || '').trim();
  if (!t) return '';
  const flat = flattenRegionOptions(ALIYUN_REGION_GROUPS);
  const hit = flat.find((o) => o.value.toLowerCase() === t.toLowerCase());
  return hit ? hit.value : t;
}

export function normalizeTencentRegion(r: string | undefined): string {
  const t = (r || '').trim();
  if (!t) return '';
  const flat = flattenRegionOptions(TENCENT_REGION_GROUPS);
  const hit = flat.find((o) => o.value.toLowerCase() === t.toLowerCase());
  return hit ? hit.value : t;
}

export function mergeUnknownRegion(
  groups: RegionGroup[],
  current: string | undefined,
): RegionGroup[] {
  const v = (current || '').trim();
  if (!v) {
    return groups;
  }
  const flat = flattenRegionOptions(groups);
  if (flat.some((o) => o.value === v)) {
    return groups;
  }
  return [
    {
      group: '当前配置',
      options: [{ value: v, label: `${v}（自定义 / 未在下列）` }],
    },
    ...groups,
  ];
}
