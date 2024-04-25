// @ts-check
// `@type` JSDoc annotations allow editor autocompletion and type checking
// (when paired with `@ts-check`).
// There are various equivalent ways to declare your Docusaurus config.
// See: https://docusaurus.io/docs/api/docusaurus-config

import {themes as prismThemes} from 'prism-react-renderer';

/** @type {import('@docusaurus/types').Config} */
const config = {
    title: 'KT2L',
    tagline: 'KT2L (ktool) is a web based tool to manage your kubernetes clusters.',
//    favicon: 'img/favicon.ico',

    onBrokenLinks: 'warn', // ideally this should have a stricter value set - 'throw'
    onBrokenMarkdownLinks: 'warn', // ideally this should have a stricter value set - 'throw'
    onDuplicateRoutes: 'warn', // ideally this should have a stricter value set - 'throw'

    // Set the production url of your site here
    url: 'https://kt2l.org/',
    // Set the /<baseUrl>/ pathname under which your site is served
    // For GitHub pages deployment, it is often '/<projectName>/'
    baseUrl: '/docs/',

    // GitHub pages deployment config.
    // If you aren't using GitHub pages, you don't need these.
    organizationName: 'KT2L', // Usually your GitHub org/user name.
    projectName: 'KT2L', // Usually your repo name.

    // Even if you don't use internationalization, you can use this field to set
    // useful metadata like html lang. For example, if your site is Chinese, you
    // may want to replace "en" with "zh-Hans".
    i18n: {
        defaultLocale: 'en',
        locales: ['en'],
    },

    presets: [
        [
            'classic',
            /** @type {import('@docusaurus/preset-classic').Options} */
            ({
                docs: {
                    path: 'docs',
                    routeBasePath: '', // change this to any URL route you'd want. For example: `home` - if you want /home/intro.
                    sidebarPath: './sidebars.js',
                    // Please change this to your repo.
                    // Remove this to remove the "edit this page" links.
                    editUrl:
                        'https://github.com/mhus/kt2l/blob/main/docs',
                },
                blog: {
                    showReadingTime: true,
                    // Please change this to your repo.
                    // Remove this to remove the "edit this page" links.
                    editUrl:
                        'https://github.com/mhus/kt2l/blob/main/docs',
                },
                theme: {
                    customCss: './src/css/custom.css',
                },
            }),
        ],
    ],

    themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
        ({
            // Replace with your project's social card
            image: 'img/docusaurus-social-card.jpg',
            docs: {
                sidebar: {
                    hideable: true
                }
            },
            navbar: {
                title: 'KT2L',
                logo: {
                    alt: 'KT2L Logo',
                    src: 'img/kt2l-logo.svg',
                    href: 'https://kt2l.org',
                    target: '_self',
                },
                items: [
                    {
                        type: 'docSidebar',
                        sidebarId: 'tutorialSidebar',
                        position: 'left',
                        label: 'Introduction',
                    },
                    {
                        to: '/get-started',
                        label: 'Get Started',
                        position: 'left'
                    },
                    {
                        href: 'https://github.com/mhus/kt2l',
                        label: 'GitHub',
                        position: 'left',
                    },
                ],
            },
            footer: {
                style: 'dark',
                links: [
                    {
                        title: 'Docs',
                        items: [
                            {
                                label: 'Introduction',
                                to: '/intro',
                            },
                            {
                                label: 'Get Started',
                                to: '/get-started',
                            },
                        ],
                    },
                    {
                        title: 'More',
                        items: [
                            {
                                label: 'Website',
                                href: 'https://kt2l.org',
                                target: '_self',
                            },
                            {
                                label: 'GitHub',
                                href: 'https://github.com/mhus/kt2l',
                            },
                        ],
                    },
                ],
                copyright: `KT2L Documentation ${new Date().getFullYear()}. Built with Docusaurus.`,
            },
            prism: {
                theme: prismThemes.github,
                darkTheme: prismThemes.dracula,
                additionalLanguages: ['java'],
            },
        }),
    markdown: {
        mermaid: true,
    },
    themes: ['@docusaurus/theme-mermaid']
};

export default config;
