# Admin notifications

Apply `migrations/20260906_device_tokens.sql`, then deploy the `admin-notify`
function. Set these server-only secrets in Supabase:

- `ADMIN_EMAILS`: comma-separated Supabase admin email allowlist
- `FCM_SERVICE_ACCOUNT_JSON`: Firebase service-account JSON

Call the function with a Supabase admin access token and a target user UUID:

```json
{"uid":"user-uuid","title":"Doomly","body":"Your daily report is ready."}
```

The service account key never ships in the APK. Firebase is used only for FCM
delivery; Supabase remains the identity and application-data provider.
