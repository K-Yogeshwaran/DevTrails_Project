import React from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

import Login          from "./pages/Login";
import Register       from "./pages/Register";
import Dashboard      from "./pages/Dashboard";
import Policy         from "./pages/Policy";
import Claims         from "./pages/Claims";
import AdminLogin     from "./pages/admin/AdminLogin";
import AdminDashboard from "./pages/admin/AdminDashboard";

const isAuthenticated = () => localStorage.getItem("token")      !== null;
const isAdminAuth     = () => localStorage.getItem("adminToken") !== null;

const Protected      = ({ children }) => isAuthenticated() ? children : <Navigate to="/"           replace />;
const AdminProtected = ({ children }) => isAdminAuth()     ? children : <Navigate to="/admin/login" replace />;

function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/"         element={<Login />} />
                <Route path="/register" element={<Register />} />

                <Route path="/dashboard" element={<Protected><Dashboard /></Protected>} />
                <Route path="/policy"    element={<Protected><Policy /></Protected>} />
                <Route path="/claims"    element={<Protected><Claims /></Protected>} />

                <Route path="/admin/login" element={<AdminLogin />} />
                <Route path="/admin"       element={<AdminProtected><AdminDashboard /></AdminProtected>} />

                <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
        </BrowserRouter>
    );
}

export default App;
