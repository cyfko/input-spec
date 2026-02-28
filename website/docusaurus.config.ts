import {themes as prismThemes} from 'prism-react-renderer';
import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';

// This runs in Node.js - Don't use client-side code here (browser APIs, JSX...)

const config: Config = {
  title: 'InputSpec',
  tagline: 'Code Once, Generate Everywhere.',
  favicon: 'img/favicon.ico',

  // Set the production url of your site here
  url: 'https://cyfko.github.io',
  // Set the /<baseUrl>/ pathname under which your site is served
  // For GitHub pages deployment, it is often '/<projectName>/'
  baseUrl: '/input-spec/',

  // GitHub pages deployment config.
  organizationName: 'cyfko', // Usually your GitHub org/user name.
  projectName: 'input-spec', // Usually your repo name.

  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',

  // i18n configuration for English and French
  i18n: {
    defaultLocale: 'en',
    locales: ['en', 'fr'],
    localeConfigs: {
      en: { label: 'English', direction: 'ltr' },
      fr: { label: 'Français', direction: 'ltr' },
    },
  },

  presets: [
    [
      'classic',
      {
        docs: {
          sidebarPath: './sidebars.ts',
          editUrl: 'https://github.com/cyfko/input-spec/tree/main/website/',
        },
        blog: false, // Disabled the blog for a documentation-focused site
        theme: {
          customCss: './src/css/custom.css',
        },
      } satisfies Preset.Options,
    ],
  ],

  themeConfig: {
    // Replace with your project's social card
    image: 'img/docusaurus-social-card.jpg',
    colorMode: {
      respectPrefersColorScheme: true,
    },
    navbar: {
      title: 'InputSpec',
      logo: {
        alt: 'InputSpec Logo',
        src: 'img/logo.svg',
      },
      items: [
        {
          type: 'docSidebar',
          sidebarId: 'tutorialSidebar',
          position: 'left',
          label: 'Documentation',
        },
        {
          type: 'localeDropdown',
          position: 'right',
        },
        {
          href: 'https://github.com/cyfko/input-spec',
          label: 'GitHub',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {
          title: 'Documentation',
          items: [
            {
              label: 'Getting Started',
              to: '/docs/intro',
            },
            {
              label: 'Essential Guide',
              to: '/docs/getting-started/hello-world',
            },
          ],
        },
        {
          title: 'More',
          items: [
            {
              label: 'GitHub Repository',
              href: 'https://github.com/cyfko/input-spec',
            },
            {
              label: 'Maven Central',
              href: 'https://central.sonatype.com/artifact/io.github.cyfko/input-spec',
            }
          ],
        },
      ],
      copyright: `Copyright © ${new Date().getFullYear()} Cyfko. Built with Docusaurus.`,
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
      additionalLanguages: ['java', 'json'],
    },
  } satisfies Preset.ThemeConfig,
};

export default config;
