/**
 * AdminLayout Component
 * Admin layout wrapper containing Header, Sidebar navigation, main content, and Footer.
 *
 * @component
 * @param {Object} props - Component props
 * @returns {React.ReactElement} Admin layout wrapper
 */
import React from 'react';
import Header from './Header';
import Sidebar from './Sidebar';
import Footer from './Footer';

const AdminLayout = ({ children }) => {
  return (
    <div className="admin-layout">
      <Header />
      <div className="admin-body">
        <Sidebar />
        <main className="admin-content">
          {children || <div className="adminlayout">AdminLayout Component</div>}
        </main>
      </div>
      <Footer />
    </div>
  );
};

export default AdminLayout;
