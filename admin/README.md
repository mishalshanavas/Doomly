# Doomly admin panel

This is a static GitHub Pages panel. It uses Supabase Google OAuth and calls the
existing `admin-notify` Edge Function; no Firebase private key is shipped here.

After Pages is enabled for the repository, open:

`https://mishalshanavas.github.io/Doomly/`

Add that exact URL to Supabase **Authentication → URL Configuration → Redirect URLs**.
Only addresses in the function's `ADMIN_EMAILS` secret can send notifications.
