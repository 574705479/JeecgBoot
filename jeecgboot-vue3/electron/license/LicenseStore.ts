import { app } from 'electron';
import * as fs from 'fs';
import * as path from 'path';

export interface LicenseData {
  licenseKey: string;
  resolvedDomain?: string;
  resolvedDomainUrl?: string;
  lastVerifyTime?: number;
}

const LICENSE_FILE = 'license.json';

function getFilePath(): string {
  return path.join(app.getPath('userData'), LICENSE_FILE);
}

export function load(): LicenseData | null {
  try {
    const filePath = getFilePath();
    if (!fs.existsSync(filePath)) return null;
    const raw = fs.readFileSync(filePath, 'utf-8');
    return JSON.parse(raw) as LicenseData;
  } catch {
    return null;
  }
}

export function save(data: LicenseData): void {
  try {
    const filePath = getFilePath();
    fs.writeFileSync(filePath, JSON.stringify(data, null, 2), 'utf-8');
  } catch (err) {
    console.error('[LicenseStore] Failed to save:', err);
  }
}

export function clear(): void {
  try {
    const filePath = getFilePath();
    if (fs.existsSync(filePath)) {
      fs.unlinkSync(filePath);
    }
  } catch (err) {
    console.error('[LicenseStore] Failed to clear:', err);
  }
}

export function hasCachedDomain(): boolean {
  const data = load();
  return !!data?.resolvedDomain;
}
