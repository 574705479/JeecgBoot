-- 更新应用：在线客服
UPDATE app SET
  app_name = '在线客服',
  app_secret = '05f0e932bf1bc4e8fdab297352952ca05d1e1fc0cd209439f3ba7d68eb5b50d8',
  app_secret_old = NULL,
  secret_rotate_at = NULL,
  public_key = '-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxUUIuUg96Llwsq8PPOac\nPa2uuy5gR+g+LnlQ6ue8vgiCy1K+Q3qA+ixxyjr8B3SwVcYXcwI+QJc1V58/ZNPR\nbDnHz4Vt3Yimgyp7mdRf5HRoRfwsUY6gJcePIb3dgDQpRPfhq7DmBB10WMeyIaE4\nlLioU9uk7XAIOl3aNEAwQnfHs9XmYvNbWrpd4F6VUuVEK4Jfttg8+Th0lf7/gSXH\nbmZUMA/BYwdyUDPKCykodgstO6r03LW4MDCvSO76zZS6jp/IieI6HfTcurjPPM3N\nGQMEKm3lVSorH7FDWJSovM4HExQBHklXn7oQzN3OY9A/sm8pSC+vM4e7F9xIJuRh\nWwIDAQAB\n-----END PUBLIC KEY-----',
  private_key = '-----BEGIN PRIVATE KEY-----\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDFRQi5SD3ouXCy\nrw885pw9ra67LmBH6D4ueVDq57y+CILLUr5DeoD6LHHKOvwHdLBVxhdzAj5AlzVX\nnz9k09FsOcfPhW3diKaDKnuZ1F/kdGhF/CxRjqAlx48hvd2ANClE9+GrsOYEHXRY\nx7IhoTiUuKhT26TtcAg6Xdo0QDBCd8ez1eZi81taul3gXpVS5UQrgl+22Dz5OHSV\n/v+BJcduZlQwD8FjB3JQM8oLKSh2Cy07qvTctbgwMK9I7vrNlLqOn8iJ4jod9Ny6\nuM88zc0ZAwQqbeVVKisfsUNYlKi8zgcTFAEeSVefuhDM3c5j0D+ybylIL68zh7sX\n3Egm5GFbAgMBAAECggEAIxb0Bc0xYfe2cbpM+hbFPP1rgtUtgCzx/p9KwvYkryLh\nbRxTRrqAPb/xH7ldQlBB24gIfMct3pxm+yaTBrLc4iTnA2VnFQxtHhDM+qB5zC/e\nqnBAS+OUXhQAxE47G8Zrft6e8ETfrWS14OLory233FB6dKwxyqBufj9ahIfUbcYD\nr5KmW3O2clpRTHr6bSfbzkuEDYkYVnX0YUWsRWQG92kyXnR836ij8YpaUJknYojQ\nB1GNRikxqww2EACbTbKvyiV2vweFrpnpNx6YDzf7pOWIinH0UwJRzhemP5UkqjEK\nbu0/i0PmplQKWDnBLYpHxM//+ffEXduzaFSg71zwrQKBgQDadJri8uU758y/mPJf\npzUGppYy4W5iyxiSWI6SaUWeRI5HqxsPb3nG6LoYMoVY/0cW4+FAh6DIK2rVw5te\niV/32LmVAYMnqDcZO/pMCuc124V3rSBCmS5FZgtbM6GxUIuCyjxrHaqz4/u0ZdMx\nxjEP4e3YLp+R16OJJeudgYo4DQKBgQDnLFDENlp6+iy6coCaBoJbAh7+gfY5XXQB\nZXt1zI6PZm2lYV9kX8L0GWPCfjsVP8BlhzhQwrJGSJw7nKfPS4eup1WHyzZJnCte\n34meHHSs+7zqgxygMt9zvj/tdB4fqE4kUc8Q6bHNONL/6WIuCkJo1PUHfYJQ/tKt\nEyHL55j9BwKBgGhDRPVSdoQr8vXzGqL6m/1Lnt4z58pxIo4ehBVu9xI9qCaejLDG\nRLnhs4f+/NPoKtbK9mQlF2DSEBBsZogtSsdxAcoTSbSwvXNmw+RtQ8LZvg7La109\n3O60cHEb6G1Hgv5uZX2+UA3Bnw2Pl8fmzt3EuA742aHy6JN5rMWk4QI1AoGBAL8U\nv1HgR5vdDHm0yZ4HAVrufuYKUJcMKyY4VvuA8NmRZdJMkUfra77O+cm7A2VGA+co\nBSGkPS2hORqBG6haGzX/dNBA5YekwaizBxKdHMgbcQU7CL9WLaODP14sG/Y47RZS\nVOD6+g/TB32m0JOTwV7boAnn8X63kzaiWM4tiiSjAoGBAKD61xUy/l3vkTREK/ra\nSKaTHg2Azc/eNVcvQI2EO95f7j5/xMWIdGkQl1QwdTgwXySIo1Quj2LXzpzKixk+\nklPR7kXbYPNXsmQcS1E3qBaFWjmEnT8IEbIguiqukTNO3Uy/PDDX0eCFMqhAplIL\nDhJS2TV7XPvXksGF1PCq2OwL\n-----END PRIVATE KEY-----',
  quotas_def = JSON_ARRAY(
    JSON_OBJECT('code','max_cs_agents','name','最大客服坐席数','type','number','defaultValue',5,'description','客服坐席数量上限，0=不限')
  ),
  features_def = JSON_ARRAY(
    JSON_OBJECT('code','system','name','系统管理','description','基础功能（所有版本包含）'),
    JSON_OBJECT('code','airag','name','AI应用平台','description','AI模型/应用/聊天/流程'),
    JSON_OBJECT('code','cs','name','在线客服','description','座席/会话/访客/工作台'),
    JSON_OBJECT('code','cs_security','name','客服安全','description','IP黑名单/登录日志')
  ),
  remark = '在线客服系统'
WHERE id = 1;

-- 更新套餐模板：体验版
UPDATE license_plan SET
  quotas = JSON_OBJECT('max_cs_agents', 3),
  features = JSON_ARRAY('system', 'cs')
WHERE app_pk = 1 AND plan_code = 'trial';

-- 更新套餐模板：基础版
UPDATE license_plan SET
  quotas = JSON_OBJECT('max_cs_agents', 10),
  features = JSON_ARRAY('system', 'cs', 'cs_security')
WHERE app_pk = 1 AND plan_code = 'basic';

-- 更新套餐模板：专业版
UPDATE license_plan SET
  quotas = JSON_OBJECT('max_cs_agents', 30),
  features = JSON_ARRAY('system', 'airag', 'cs', 'cs_security')
WHERE app_pk = 1 AND plan_code = 'professional';

-- 更新套餐模板：企业版（0=不限）
UPDATE license_plan SET
  quotas = JSON_OBJECT('max_cs_agents', 0),
  features = JSON_ARRAY('system', 'airag', 'cs', 'cs_security')
WHERE app_pk = 1 AND plan_code = 'enterprise';
