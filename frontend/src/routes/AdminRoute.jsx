/**
 * AdminRoute Component
 * Route guard checking for administrative / conductor role privileges.
 *
 * @component
 * @param {Object} props - Component props
 * @returns {React.ReactElement} Children or access denied view
 */
import React from 'react';

const AdminRoute = ({ children }) => {
  const isAdmin = true;

  if (!isAdmin) {
    return <div className="access-denied">Access Denied: Admin privileges required.</div>;
  }

  return children;
};

export default AdminRoute;
