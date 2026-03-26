import * as https from 'https';
import * as http from 'http';
import { URL } from 'url';

export interface DomainConfig {
  apiUrl: string;
  domainUrl: string;
}

export interface DomainsResponse {
  code: number;
  data?: {
    status: string;
    domains: string;
  };
  message?: string;
}

function httpGet(url: string, timeoutMs = 5000): Promise<string> {
  return new Promise((resolve, reject) => {
    const parsed = new URL(url);
    const mod = parsed.protocol === 'https:' ? https : http;
    const req = mod.get(url, { timeout: timeoutMs }, (res) => {
      let body = '';
      res.on('data', (chunk) => (body += chunk));
      res.on('end', () => resolve(body));
    });
    req.on('error', reject);
    req.on('timeout', () => {
      req.destroy();
      reject(new Error('timeout'));
    });
  });
}

function normalizeDomain(raw: string): string {
  let d = raw.trim();
  if (!d) return '';
  if (!/^https?:\/\//i.test(d)) {
    d = 'https://' + d;
  }
  return d.replace(/\/+$/, '');
}

export async function fetchDomains(
  licenseUrl: string,
  licenseKey: string
): Promise<DomainsResponse> {
  const url = `${licenseUrl.replace(/\/+$/, '')}/api/v1/license/domains?licenseKey=${encodeURIComponent(licenseKey)}`;
  const body = await httpGet(url, 10000);
  return JSON.parse(body) as DomainsResponse;
}

export async function testDomainSpeed(domain: string): Promise<number> {
  const url = normalizeDomain(domain) + '/';
  const start = Date.now();
  await httpGet(url, 5000);
  return Date.now() - start;
}

interface DomainTestResult {
  domain: string;
  latency: number;
}

export async function resolveBestDomain(
  rawDomains: string
): Promise<DomainConfig | null> {
  const list = rawDomains
    .split(/[\n\r]+/)
    .map((d) => d.trim())
    .filter(Boolean);

  if (list.length === 0) return null;

  const results: DomainTestResult[] = [];
  const tests = list.map(async (domain) => {
    try {
      const latency = await testDomainSpeed(domain);
      results.push({ domain, latency });
    } catch {
      console.warn(`[DomainResolver] Domain ${domain} unreachable`);
    }
  });
  await Promise.all(tests);

  if (results.length === 0) return null;

  results.sort((a, b) => a.latency - b.latency);
  const best = normalizeDomain(results[0].domain);
  console.log(
    `[DomainResolver] Best domain: ${best} (${results[0].latency}ms)`
  );
  return {
    apiUrl: best,
    domainUrl: best + '/jeecgboot',
  };
}
