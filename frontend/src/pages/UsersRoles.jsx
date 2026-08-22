import { useEffect, useState } from "react";
import {
  Users,
  Shield,
  Plus,
  Trash2,
  Edit2,
  Search,
  CheckCircle2,
  XCircle,
  Loader2,
  Key,
  ShieldAlert,
  UserCheck,
} from "lucide-react";
import Topbar from "../components/layout/Topbar";
import { usersApi } from "../api";
import { useAuth } from "../context/AuthContext";

const ROLES = ["ADMIN", "COMPLIANCE_OFFICER", "ENGINEER", "VIEWER"];

const ROLE_BADGE = {
  ADMIN: "bg-purple-500/15 text-purple-400 border border-purple-500/30",
  COMPLIANCE_OFFICER: "bg-amber-500/15 text-amber-400 border border-amber-500/30",
  ENGINEER: "bg-blue-500/15 text-blue-400 border border-blue-500/30",
  VIEWER: "bg-zinc-500/15 text-zinc-300 border border-zinc-500/30",
};

export default function UsersRoles() {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState("directory"); // "directory" | "matrix"

  // Filters
  const [search, setSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState("ALL");

  // Modal States
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [createForm, setCreateForm] = useState({ email: "", password: "", role: "ENGINEER", status: "ACTIVE" });
  const [createSubmitting, setCreateSubmitting] = useState(false);
  const [createError, setCreateError] = useState(null);

  const [editingUser, setEditingUser] = useState(null);
  const [editForm, setEditForm] = useState({ role: "", status: "" });
  const [editSubmitting, setEditSubmitting] = useState(false);
  const [editError, setEditError] = useState(null);

  const [deletingUser, setDeletingUser] = useState(null);
  const [deleteSubmitting, setDeleteSubmitting] = useState(false);

  const isAdmin = currentUser?.role === "ADMIN";

  async function loadData() {
    setLoading(true);
    setError(null);
    try {
      const [usersData, rolesData] = await Promise.allSettled([
        usersApi.list(),
        usersApi.roles(),
      ]);
      if (usersData.status === "fulfilled") setUsers(usersData.value);
      else setError(usersData.reason?.message || "Failed to load users");
      if (rolesData.status === "fulfilled") setRoles(rolesData.value);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadData();
  }, []);

  async function handleCreateUser(e) {
    e.preventDefault();
    if (!createForm.email.trim() || !createForm.password.trim()) {
      setCreateError("Email and password are required.");
      return;
    }
    if (createForm.password.length < 8) {
      setCreateError("Password must be at least 8 characters.");
      return;
    }
    setCreateSubmitting(true);
    setCreateError(null);
    try {
      await usersApi.create({
        email: createForm.email.trim().toLowerCase(),
        password: createForm.password,
        role: createForm.role,
        status: createForm.status,
      });
      setShowCreateModal(false);
      setCreateForm({ email: "", password: "", role: "ENGINEER", status: "ACTIVE" });
      await loadData();
    } catch (err) {
      setCreateError(err.message);
    } finally {
      setCreateSubmitting(false);
    }
  }

  async function handleUpdateUser(e) {
    e.preventDefault();
    if (!editingUser) return;
    setEditSubmitting(true);
    setEditError(null);
    try {
      await usersApi.update(editingUser.id, {
        role: editForm.role,
        status: editForm.status,
      });
      setEditingUser(null);
      await loadData();
    } catch (err) {
      setEditError(err.message);
    } finally {
      setEditSubmitting(false);
    }
  }

  async function handleDeleteUser() {
    if (!deletingUser) return;
    setDeleteSubmitting(true);
    try {
      await usersApi.remove(deletingUser.id);
      setDeletingUser(null);
      await loadData();
    } catch (err) {
      setError(err.message);
    } finally {
      setDeleteSubmitting(false);
    }
  }

  function openEditModal(u) {
    setEditingUser(u);
    setEditForm({ role: u.role, status: u.status || "ACTIVE" });
    setEditError(null);
  }

  const filteredUsers = users.filter((u) => {
    const matchSearch = u.email.toLowerCase().includes(search.toLowerCase());
    const matchRole = roleFilter === "ALL" || u.role === roleFilter;
    const matchStatus = statusFilter === "ALL" || u.status === statusFilter;
    return matchSearch && matchRole && matchStatus;
  });

  const activeCount = users.filter((u) => u.status === "ACTIVE").length;
  const adminCount = users.filter((u) => u.role === "ADMIN").length;

  return (
    <div>
      <Topbar
        title="Users & Roles"
        subtitle="Manage platform identities, credential access, and Role-Based Access Controls (RBAC)."
      />

      <div className="px-6 lg:px-8 mt-4">
        {/* KPI Cards */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
          <div className="card p-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-purple-500/10 flex items-center justify-center text-purple-400">
                <Users size={20} />
              </div>
              <div>
                <p className="text-xs text-[var(--color-text-faint)]">Total Users</p>
                <p className="text-xl font-bold text-white">{loading ? "—" : users.length}</p>
              </div>
            </div>
          </div>
          <div className="card p-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-green-500/10 flex items-center justify-center text-green-400">
                <UserCheck size={20} />
              </div>
              <div>
                <p className="text-xs text-[var(--color-text-faint)]">Active Accounts</p>
                <p className="text-xl font-bold text-white">{loading ? "—" : activeCount}</p>
              </div>
            </div>
          </div>
          <div className="card p-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-blue-500/10 flex items-center justify-center text-blue-400">
                <Shield size={20} />
              </div>
              <div>
                <p className="text-xs text-[var(--color-text-faint)]">Admin Users</p>
                <p className="text-xl font-bold text-white">{loading ? "—" : adminCount}</p>
              </div>
            </div>
          </div>
          <div className="card p-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-amber-500/10 flex items-center justify-center text-amber-400">
                <Key size={20} />
              </div>
              <div>
                <p className="text-xs text-[var(--color-text-faint)]">Access Roles</p>
                <p className="text-xl font-bold text-white">{roles.length || 4}</p>
              </div>
            </div>
          </div>
        </div>

        {/* View Tabs & Action Button */}
        <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
          <div className="flex items-center bg-[var(--color-surface)] border border-[var(--color-border)] rounded-xl p-1">
            <button
              onClick={() => setActiveTab("directory")}
              className={`px-4 py-1.5 text-xs font-medium rounded-lg transition-colors ${
                activeTab === "directory"
                  ? "bg-[var(--color-brand)] text-white"
                  : "text-[var(--color-text-dim)] hover:text-white"
              }`}
            >
              User Directory
            </button>
            <button
              onClick={() => setActiveTab("matrix")}
              className={`px-4 py-1.5 text-xs font-medium rounded-lg transition-colors ${
                activeTab === "matrix"
                  ? "bg-[var(--color-brand)] text-white"
                  : "text-[var(--color-text-dim)] hover:text-white"
              }`}
            >
              RBAC Permission Matrix
            </button>
          </div>

          {isAdmin && (
            <button
              onClick={() => setShowCreateModal(true)}
              className="btn-primary flex items-center gap-1.5"
            >
              <Plus size={15} /> Add New User
            </button>
          )}
        </div>

        {error && (
          <div className="rounded-xl bg-[var(--color-bad)]/10 border border-[var(--color-bad)]/30 px-4 py-3 text-sm text-[var(--color-bad)] mb-4 flex items-center justify-between">
            {error}
            <button onClick={loadData} className="underline ml-4 text-xs">Retry</button>
          </div>
        )}

        {/* Tab 1: User Directory */}
        {activeTab === "directory" && (
          <div className="space-y-4 pb-8">
            {/* Search & Filters */}
            <div className="card p-4 flex flex-wrap items-center justify-between gap-3">
              <div className="relative min-w-[240px] flex-1 max-w-md">
                <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--color-text-faint)]" />
                <input
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  placeholder="Search users by email..."
                  className="field-input pl-9"
                />
              </div>

              <div className="flex items-center gap-3">
                <div className="flex items-center gap-2">
                  <span className="text-xs text-[var(--color-text-faint)]">Role:</span>
                  <select
                    value={roleFilter}
                    onChange={(e) => setRoleFilter(e.target.value)}
                    className="field-input py-1.5 text-xs"
                  >
                    <option value="ALL">All Roles</option>
                    {ROLES.map((r) => (
                      <option key={r} value={r}>{r}</option>
                    ))}
                  </select>
                </div>

                <div className="flex items-center gap-2">
                  <span className="text-xs text-[var(--color-text-faint)]">Status:</span>
                  <select
                    value={statusFilter}
                    onChange={(e) => setStatusFilter(e.target.value)}
                    className="field-input py-1.5 text-xs"
                  >
                    <option value="ALL">All Statuses</option>
                    <option value="ACTIVE">ACTIVE</option>
                    <option value="INACTIVE">INACTIVE</option>
                  </select>
                </div>
              </div>
            </div>

            {/* Table */}
            <div className="card overflow-hidden">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-[var(--color-text-faint)] border-b border-[var(--color-border)]">
                    <th className="px-5 py-3 font-medium">User Identity</th>
                    <th className="px-5 py-3 font-medium">Role</th>
                    <th className="px-5 py-3 font-medium">Status</th>
                    <th className="px-5 py-3 font-medium">Created Date</th>
                    <th className="px-5 py-3 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {loading && (
                    <tr>
                      <td colSpan={5} className="px-5 py-10 text-center text-[var(--color-text-faint)]">
                        <Loader2 size={18} className="animate-spin inline mr-2" /> Loading users...
                      </td>
                    </tr>
                  )}
                  {!loading && filteredUsers.length === 0 && (
                    <tr>
                      <td colSpan={5} className="px-5 py-10 text-center text-[var(--color-text-faint)]">
                        No users match the search criteria.
                      </td>
                    </tr>
                  )}
                  {!loading &&
                    filteredUsers.map((u) => {
                      const isSelf = u.email.toLowerCase() === currentUser?.email?.toLowerCase();
                      return (
                        <tr
                          key={u.id}
                          className="border-b border-[var(--color-border)] last:border-0 hover:bg-[var(--color-surface-2)] transition-colors"
                        >
                          <td className="px-5 py-3.5">
                            <div className="flex items-center gap-3">
                              <div className="w-8 h-8 rounded-full bg-gradient-to-br from-[#7c6cf9] to-[#5b3df0] flex items-center justify-center text-xs font-semibold text-white">
                                {u.email.slice(0, 2).toUpperCase()}
                              </div>
                              <div>
                                <p className="font-medium text-white text-sm">
                                  {u.email} {isSelf && <span className="text-xs text-[var(--color-brand)] font-normal">(You)</span>}
                                </p>
                                <p className="text-[11px] text-[var(--color-text-faint)]">ID #{u.id}</p>
                              </div>
                            </div>
                          </td>
                          <td className="px-5 py-3.5">
                            <span className={`text-xs font-semibold px-2.5 py-1 rounded-lg ${ROLE_BADGE[u.role] || ROLE_BADGE.VIEWER}`}>
                              {u.role}
                            </span>
                          </td>
                          <td className="px-5 py-3.5">
                            <span
                              className={`text-xs font-medium px-2 py-0.5 rounded-md ${
                                u.status === "ACTIVE"
                                  ? "bg-[var(--color-good)]/15 text-[var(--color-good)]"
                                  : "bg-[var(--color-bad)]/15 text-[var(--color-bad)]"
                              }`}
                            >
                              {u.status || "ACTIVE"}
                            </span>
                          </td>
                          <td className="px-5 py-3.5 text-xs text-[var(--color-text-dim)]">
                            {u.createdAt ? new Date(u.createdAt).toLocaleDateString() : "—"}
                          </td>
                          <td className="px-5 py-3.5 text-right">
                            <div className="flex items-center justify-end gap-2">
                              {isAdmin && (
                                <button
                                  onClick={() => openEditModal(u)}
                                  className="text-[var(--color-text-faint)] hover:text-white p-1 rounded transition-colors"
                                  title="Edit User"
                                >
                                  <Edit2 size={15} />
                                </button>
                              )}
                              {isAdmin && !isSelf && (
                                <button
                                  onClick={() => setDeletingUser(u)}
                                  className="text-[var(--color-text-faint)] hover:text-[var(--color-bad)] p-1 rounded transition-colors"
                                  title="Delete User"
                                >
                                  <Trash2 size={15} />
                                </button>
                              )}
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* Tab 2: RBAC Matrix */}
        {activeTab === "matrix" && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pb-8">
            {roles.map((r) => (
              <div key={r.role} className="card p-5 space-y-4">
                <div className="flex items-start justify-between">
                  <div>
                    <span className={`text-xs font-semibold px-2.5 py-1 rounded-lg ${ROLE_BADGE[r.role] || ROLE_BADGE.VIEWER}`}>
                      {r.role}
                    </span>
                    <h3 className="text-base font-semibold text-white mt-2">{r.title}</h3>
                  </div>
                  <Shield size={22} className="text-[var(--color-text-faint)]" />
                </div>
                <p className="text-xs text-[var(--color-text-dim)] leading-relaxed">{r.description}</p>
                <div className="border-t border-[var(--color-border)] pt-3">
                  <p className="text-xs font-medium text-white mb-2">Granted Permissions</p>
                  <ul className="space-y-1.5">
                    {r.permissions.map((p, i) => (
                      <li key={i} className="flex items-center gap-2 text-xs text-[var(--color-text-dim)]">
                        <CheckCircle2 size={13} className="text-[var(--color-good)] shrink-0" />
                        <span>{p}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Modal: Create User */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="card w-full max-w-md p-6 space-y-4 animate-in fade-in zoom-in-95">
            <h2 className="text-lg font-semibold text-white">Add New User</h2>
            <form onSubmit={handleCreateUser} className="space-y-4">
              <div>
                <label className="block text-xs text-[var(--color-text-dim)] mb-1">Email Address *</label>
                <input
                  type="email"
                  value={createForm.email}
                  onChange={(e) => setCreateForm({ ...createForm, email: e.target.value })}
                  placeholder="analyst@policymesh.io"
                  className="field-input"
                  required
                />
              </div>
              <div>
                <label className="block text-xs text-[var(--color-text-dim)] mb-1">Initial Password * (min 8 chars)</label>
                <input
                  type="password"
                  value={createForm.password}
                  onChange={(e) => setCreateForm({ ...createForm, password: e.target.value })}
                  placeholder="••••••••••••"
                  className="field-input"
                  required
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs text-[var(--color-text-dim)] mb-1">Assigned Role *</label>
                  <select
                    value={createForm.role}
                    onChange={(e) => setCreateForm({ ...createForm, role: e.target.value })}
                    className="field-input"
                  >
                    {ROLES.map((r) => (
                      <option key={r} value={r}>{r}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-xs text-[var(--color-text-dim)] mb-1">Account Status</label>
                  <select
                    value={createForm.status}
                    onChange={(e) => setCreateForm({ ...createForm, status: e.target.value })}
                    className="field-input"
                  >
                    <option value="ACTIVE">ACTIVE</option>
                    <option value="INACTIVE">INACTIVE</option>
                  </select>
                </div>
              </div>

              {createError && (
                <p className="text-xs text-[var(--color-bad)] bg-[var(--color-bad)]/10 rounded-lg px-3 py-2">
                  {createError}
                </p>
              )}

              <div className="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="btn-ghost"
                >
                  Cancel
                </button>
                <button type="submit" disabled={createSubmitting} className="btn-primary">
                  {createSubmitting ? <Loader2 size={14} className="animate-spin" /> : <Plus size={14} />}
                  Create Account
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Edit User */}
      {editingUser && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="card w-full max-w-md p-6 space-y-4 animate-in fade-in zoom-in-95">
            <h2 className="text-lg font-semibold text-white">Edit User: {editingUser.email}</h2>
            <form onSubmit={handleUpdateUser} className="space-y-4">
              <div>
                <label className="block text-xs text-[var(--color-text-dim)] mb-1">Role</label>
                <select
                  value={editForm.role}
                  onChange={(e) => setEditForm({ ...editForm, role: e.target.value })}
                  className="field-input"
                >
                  {ROLES.map((r) => (
                    <option key={r} value={r}>{r}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-xs text-[var(--color-text-dim)] mb-1">Status</label>
                <select
                  value={editForm.status}
                  onChange={(e) => setEditForm({ ...editForm, status: e.target.value })}
                  className="field-input"
                >
                  <option value="ACTIVE">ACTIVE</option>
                  <option value="INACTIVE">INACTIVE</option>
                </select>
              </div>

              {editError && (
                <p className="text-xs text-[var(--color-bad)] bg-[var(--color-bad)]/10 rounded-lg px-3 py-2">
                  {editError}
                </p>
              )}

              <div className="flex justify-end gap-3 pt-2">
                <button type="button" onClick={() => setEditingUser(null)} className="btn-ghost">
                  Cancel
                </button>
                <button type="submit" disabled={editSubmitting} className="btn-primary">
                  {editSubmitting ? <Loader2 size={14} className="animate-spin" /> : "Save Changes"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Delete User Confirmation */}
      {deletingUser && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="card w-full max-w-md p-6 space-y-4 border-[var(--color-bad)]/30 animate-in fade-in zoom-in-95">
            <div className="flex items-center gap-3 text-[var(--color-bad)]">
              <ShieldAlert size={24} />
              <h2 className="text-lg font-semibold text-white">Confirm Deletion</h2>
            </div>
            <p className="text-sm text-[var(--color-text-dim)]">
              Are you sure you want to delete user <strong className="text-white">{deletingUser.email}</strong>? This action cannot be undone.
            </p>
            <div className="flex justify-end gap-3 pt-2">
              <button type="button" onClick={() => setDeletingUser(null)} className="btn-ghost">
                Cancel
              </button>
              <button
                type="button"
                onClick={handleDeleteUser}
                disabled={deleteSubmitting}
                className="btn-primary bg-[var(--color-bad)] hover:bg-[var(--color-bad)]/80"
              >
                {deleteSubmitting ? <Loader2 size={14} className="animate-spin" /> : <Trash2 size={14} />}
                Delete User
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
