/**
 * MainLayout Component
 * Primary layout wrapper containing Header, main content area, and Footer.
 *
 * @component
 * @param {Object} props - Component props
 * @returns {React.ReactElement} Main layout wrapper
 */
import React from 'react';
import Header from './Header';
import Footer from './Footer';

const MainLayout = ({ children }) => {
  return (
    <div className="main-layout">
      <Header />
      <main className="main-content">
        {children || <div className="mainlayout">MainLayout Component</div>}
      </main>
      <Footer />
    </div>
  );
};

export default MainLayout;
