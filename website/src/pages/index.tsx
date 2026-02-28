import type {ReactNode} from 'react';
import clsx from 'clsx';
import Link from '@docusaurus/Link';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';
import Heading from '@theme/Heading';
import Translate, {translate} from '@docusaurus/Translate';

import styles from './index.module.css';

function HomepageHeader() {
  const {siteConfig} = useDocusaurusContext();
  return (
    <header className={clsx('hero hero--primary', styles.heroBanner)}>
      <div className="container">
        <Heading as="h1" className="hero__title">
          {siteConfig.title}
        </Heading>
        <p className="hero__subtitle">
          <Translate id="homepage.tagline">
            Code Once, Generate Everywhere.
          </Translate>
        </p>
        <div className={styles.buttons}>
          <Link
            className="button button--secondary button--lg"
            to="/docs/intro">
            <Translate id="homepage.getstarted">Get Started</Translate>
          </Link>
          <span style={{margin: '0 10px'}}></span>
          <Link
            className="button button--outline button--secondary button--lg"
            to="/docs/mcp/agents-and-forms">
            <Translate id="homepage.mcp">AI & MCP Ready</Translate>
          </Link>
        </div>
      </div>
    </header>
  );
}

function FeatureList() {
  return (
    <section className={styles.features}>
      <div className="container">
        <div className="row">
          <div className={clsx('col col--4')}>
            <div className="text--center padding-horiz--md">
              <Heading as="h3">
                <Translate id="homepage.features.ssot.title">Single Source of Truth</Translate>
              </Heading>
              <p>
                <Translate id="homepage.features.ssot.desc">
                  Define your form structure and validation rules exactly once in your backend Java code using standard Jakarta annotations. No more duplicating regexes in JavaScript.
                </Translate>
              </p>
            </div>
          </div>
          <div className={clsx('col col--4')}>
            <div className="text--center padding-horiz--md">
              <Heading as="h3">
                <Translate id="homepage.features.framework.title">Framework Agnostic</Translate>
              </Heading>
              <p>
                <Translate id="homepage.features.framework.desc">
                  InputSpec generates a standard JSON protocol (DIFSP). Consume it in React, Vue, Angular, Flutter, or iOS. Your backend UI logic becomes perfectly portable.
                </Translate>
              </p>
            </div>
          </div>
          <div className={clsx('col col--4')}>
            <div className="text--center padding-horiz--md">
              <Heading as="h3">
                <Translate id="homepage.features.ai.title">AI Native</Translate>
              </Heading>
              <p>
                <Translate id="homepage.features.ai.desc">
                  Because forms are described in structured JSON, AI agents can autonomously discover, understand, and fill out complex application forms using the Model Context Protocol (MCP).
                </Translate>
              </p>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

export default function Home(): ReactNode {
  const {siteConfig} = useDocusaurusContext();
  return (
    <Layout
      title={`${siteConfig.title}`}
      description="The definitive guide to the InputSpec framework and DIFSP protocol.">
      <HomepageHeader />
      <main>
        <FeatureList />
      </main>
    </Layout>
  );
}
