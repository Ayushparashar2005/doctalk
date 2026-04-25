import { defineConfig } from 'astro/config';
import react from '@astrojs/react';
import cloudflare from '@astrojs/cloudflare';

export default defineConfig({
  site: 'https://doctalk.pages.dev',
  adapter: cloudflare({
    imageService: 'compile'
  }),
  integrations: [react()]
});
