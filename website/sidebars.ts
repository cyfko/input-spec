import type {SidebarsConfig} from '@docusaurus/plugin-content-docs';

const sidebars: SidebarsConfig = {
  tutorialSidebar: [
    {
      type: 'category',
      label: 'Introduction',
      collapsed: false,
      items: [
        'intro',
        'getting-started/hello-world',
      ],
    },
    {
      type: 'category',
      label: 'Essential Guide',
      collapsed: false,
      items: [
        'essential-guide/forms',
        'essential-guide/validation',
        'essential-guide/values-sources',
      ],
    },
    {
      type: 'category',
      label: 'Advanced Guide',
      collapsed: true,
      items: [
        'advanced-guide/cross-constraints',
        'advanced-guide/custom-handlers',
        'advanced-guide/i18n',
      ],
    },
    {
      type: 'category',
      label: 'AI & MCP Integration',
      collapsed: false,
      items: [
        'mcp/agents-and-forms',
        'mcp/spring-boot-demo',
      ],
    },
    {
      type: 'category',
      label: 'Protocol Reference',
      collapsed: true,
      items: [
        'protocol/specification',
      ],
    },
  ],
};

export default sidebars;
