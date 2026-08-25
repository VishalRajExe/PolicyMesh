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
  Lock,
  UserCheck,
  AlertTriangle,
} from "lucide-react";
import Topbar, { TopbarActions } from "../components/layout/Topbar";
import Pagination from "../components/ui/Pagination";
import Button from "../components/ui/Button";
import Badge from "../components/ui/Badge";
import Modal from "../components/ui/Modal";
import EmptyState from "../components/ui/EmptyState";
import Tabs from "../components/ui/Tabs";
import { TableSkeleton } from "../components/ui/LoadingSkeleton";
import { usersApi } from "../api";
import { useAuth } from "../context/AuthContext";

const ROLES = ["ADMIN", "COMPLIANCE_OFFICER", "ENGINEER", "VIEWER"];

export default function UsersRoles() {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Tabs & filters
  const [activeTab, setActiveTab] = useState("directory"); // "directory" | "matrix"
  const [search, setSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  // Modals
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
      const [usersData, rolesData] = await Promise.allSettled([usersApi.list(), usersApi.roles()]);
      if (usersData.status === "fulfilled") setUsers(Array.isArray(usersData.value) ? usersData.value : []);
      else setError(usersData.reason?.message || "Failed to load users");
      if (rolesData.status === "fulfilled") setRoles(Array.isArray(rolesData.value) ? rolesData.value : []);
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
      setCreateError(err.message || "Failed to create user.");
    } finally {
      setCreateSubmitting(false);
    }
  }

  function openEditUser(u) {
    setEditingUser(u);
    setEditForm({ role: u.role || "VIEWER", status: u.status || "ACTIVE" });
    setEditError(null);
  }

  async function handleEditUser(e) {
    e.preventDefault();
    if (!editingUser) return;
    setEditSubmitting(true);
    setEditError(null);
    try {
      await usersApi.updateRole(editingUser.id, editForm.role);
      setEditingUser(null);
      await loadData();
    } catch (err) {
      setEditError(err.message || "Failed to update user.");
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
      setError(err.message || "Failed to delete user.");
    } finally {
      setDeleteSubmitting(false);
    }
  }

  const filteredUsers = users.filter((u) => {
    const q = search.trim().toLowerCase();
    const matchSearch = !q || (u.email || "").toLowerCase().includes(q) || (u.role || "").toLowerCase().includes(q);
    const matchRole = roleFilter === "ALL" || u.role === roleFilter;
    const matchStatus = statusFilter === "ALL" || u.status === statusFilter;
    return matchSearch && matchRole && matchStatus;
  });

  const paginatedUsers = filteredUsers.slice((page - 1) * pageSize, page * pageSize);

  const topActions = isAdmin && (
    <Button variant="primary" size="md" icon={Plus} onClick={() => setShowCreateModal(true)}>
      Add User
    </Button>
  );

  return (
    <div>
      <Topbar
        title="Users & Access Control"
        subtitle="Manage organization team members, role assignments, and granular RBAC permissions."
        actions={topActions}
      />

      <div className="px-4 sm:px-6 lg:px-8 py-4 sm:py-6 space-y-4 pb-12">
        {/* Navigation Tabs */}
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
          <Tabs
            tabs={[
              { id: "directory", label: "User Directory", icon: Users, count: users.length },
              { id: "matrix", label: "RBAC Permissions Matrix", icon: Shield },
            ]}
            activeTab={activeTab}
            onChange={setActiveTab}
          />
        </div>

        {activeTab === "directory" ? (
          <div className="space-y-4">
            {/* Filter Bar */}
            <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
              <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2.5 flex-1 w-full sm:max-w-2xl">
                <div className="relative flex-1 min-w-[200px]">
                  <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--color-text-faint)] pointer-events-none" />
                  <input
                    value={search}
                    onChange={(e) => {
                      setSearch(e.target.value);
                      setPage(1);
                    }}
                    placeholder="Search users..."
                    className="field-input field-input-search !pl-9 text-xs"
                  />
                  {search && (
                    <button
                      type="button"
                      onClick={() => setSearch("")}
                      className="absolute right-2.5 top-1/2 -translate-y-1/2 text-[var(--color-text-faint)] hover:text-[var(--color-text)] p-0.5"
                      title="Clear search"
                    >
                      <X size={12} />
                    </button>
                  )}
                </div>

                <div className="flex items-center gap-2">
                  <select
                    value={roleFilter}
                    onChange={(e) => {
                      setRoleFilter(e.target.value);
                      setPage(1);
                    }}
                    className="field-input py-1.5 text-xs flex-1 sm:w-36"
                  >
                    <option value="ALL">All Roles</option>
                    {ROLES.map((r) => (
                      <option key={r} value={r}>
                        {r}
                      </option>
                    ))}
                  </select>

                  <select
                    value={statusFilter}
                    onChange={(e) => {
                      setStatusFilter(e.target.value);
                      setPage(1);
                    }}
                    className="field-input py-1.5 text-xs flex-1 sm:w-32"
                  >
                    <option value="ALL">All Statuses</option>
                    <option value="ACTIVE">Active</option>
                    <option value="DISABLED">Disabled</option>
                  </select>
                </div>
              </div>

              <div className="flex items-center justify-between sm:justify-end gap-3">
                {isAdmin && (
                  <div className="block sm:hidden flex-1">
                    <Button variant="primary" size="sm" icon={Plus} className="w-full justify-center" onClick={() => setShowCreateModal(true)}>
                      Add User
                    </Button>
                  </div>
                )}
                <span className="text-xs text-[var(--color-text-faint)] font-mono self-center">
                  {filteredUsers.length} {filteredUsers.length === 1 ? "user" : "users"}
                </span>
              </div>
            </div>

            {/* Table Card */}
            <div className="card overflow-hidden">
              {loading ? (
                <TableSkeleton rows={5} cols={5} />
              ) : filteredUsers.length === 0 ? (
                <EmptyState
                  icon={Users}
                  title="No users match your criteria"
                  description="Adjust your search filters or add a new team member."
                  actionLabel={isAdmin ? "Add User" : null}
                  onAction={() => setShowCreateModal(true)}
                  actionIcon={Plus}
                />
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs border-collapse">
                    <thead className="bg-[var(--color-surface-2)] text-[var(--color-text-dim)] border-b border-[var(--color-border)] uppercase tracking-wider font-semibold text-[10px]">
                      <tr>
                        <th className="px-5 py-3 font-semibold">User Member</th>
                        <th className="px-5 py-3 font-semibold">Role</th>
                        <th className="px-5 py-3 font-semibold">Status</th>
                        <th className="px-5 py-3 font-semibold">Created</th>
                        <th className="px-5 py-3 font-semibold text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-[var(--color-border)]">
                      {paginatedUsers.map((u) => {
                        const isSelf = u.id === currentUser?.id;
                        const initials = (u.email || "U").slice(0, 2).toUpperCase();

                        return (
                          <tr key={u.id} className="hover:bg-[var(--color-surface-2)]/60 transition-colors">
                            <td className="px-5 py-3">
                              <div className="flex items-center gap-2.5">
                                <div className="w-7 h-7 rounded-full bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-[10px] font-bold text-white shadow-2xs shrink-0">
                                  {initials}
                                </div>
                                <div>
                                  <span className="font-semibold text-xs text-[var(--color-text)] block">
                                    {u.email}
                                  </span>
                                  {isSelf && (
                                    <span className="text-[10px] text-[var(--color-brand)] font-medium">
                                      (You)
                                    </span>
                                  )}
                                </div>
                              </div>
                            </td>

                            <td className="px-5 py-3">
                              <Badge
                                variant={
                                  u.role === "ADMIN"
                                    ? "brand"
                                    : u.role === "COMPLIANCE_OFFICER"
                                    ? "warn"
                                    : "info"
                                }
                                size="sm"
                              >
                                {u.role || "VIEWER"}
                              </Badge>
                            </td>

                            <td className="px-5 py-3">
                              <Badge
                                variant={u.status === "DISABLED" ? "bad" : "good"}
                                size="sm"
                                dot
                              >
                                {u.status || "ACTIVE"}
                              </Badge>
                            </td>

                            <td className="px-5 py-3 text-[11px] text-[var(--color-text-faint)] whitespace-nowrap">
                              {u.createdAt ? new Date(u.createdAt).toLocaleDateString() : "Active"}
                            </td>

                            <td className="px-5 py-3 text-right">
                              {isAdmin && (
                                <div className="flex items-center justify-end gap-1.5">
                                  <button
                                    onClick={() => openEditUser(u)}
                                    className="p-1 rounded-lg text-[var(--color-text-faint)] hover:text-[var(--color-text)] hover:bg-[var(--color-surface-2)] transition-colors"
                                    title="Edit Role / Status"
                                  >
                                    <Edit2 size={14} />
                                  </button>
                                  {!isSelf && (
                                    <button
                                      onClick={() => setDeletingUser(u)}
                                      className="p-1 rounded-lg text-[var(--color-text-faint)] hover:text-[var(--color-bad)] hover:bg-[var(--color-bad-light)] transition-colors"
                                      title="Delete User"
                                    >
                                      <Trash2 size={14} />
                                    </button>
                                  )}
                                </div>
                              )}
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>

                  <Pagination
                    currentPage={page}
                    totalItems={filteredUsers.length}
                    pageSize={pageSize}
                    onPageChange={setPage}
                    onPageSizeChange={(sz) => {
                      setPageSize(sz);
                      setPage(1);
                    }}
                  />
                </div>
              )}
            </div>
          </div>
        ) : (
          /* RBAC Permissions Matrix Card */
          <div className="card p-5 space-y-4">
            <div>
              <h3 className="font-bold text-sm text-[var(--color-text)]">Role Permissions Matrix</h3>
              <p className="text-xs text-[var(--color-text-dim)] mt-0.5">
                Overview of capabilities and administrative entitlements per RBAC tier.
              </p>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs border-collapse">
                <thead className="bg-[var(--color-surface-2)] text-[var(--color-text-dim)] border-b border-[var(--color-border)] uppercase tracking-wider font-semibold text-[10px]">
                  <tr>
                    <th className="px-5 py-3">Permission Area</th>
                    <th className="px-5 py-3 text-center">Admin</th>
                    <th className="px-5 py-3 text-center">Compliance Officer</th>
                    <th className="px-5 py-3 text-center">Engineer</th>
                    <th className="px-5 py-3 text-center">Viewer</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[var(--color-border)]">
                  {[
                    { name: "View Dashboard & Real-Time Alerts", a: true, co: true, e: true, v: true },
                    { name: "Manage Policy Definitions (Create/Edit/Delete)", a: true, co: true, e: false, v: false },
                    { name: "Register Service Nodes & Topology Edges", a: true, co: false, e: true, v: false },
                    { name: "Trigger Runtime Enforcement Simulations", a: true, co: true, e: true, v: true },
                    { name: "Approve / Reject AI Schema Classifications", a: true, co: true, e: false, v: false },
                    { name: "Execute Pre-Merge CI Checks", a: true, co: true, e: true, v: true },
                    { name: "Export Compliance Audit Reports (CSV)", a: true, co: true, e: true, v: true },
                    { name: "Manage Users & Grant RBAC Roles", a: true, co: false, e: false, v: false },
                  ].map((row, idx) => (
                    <tr key={idx} className="hover:bg-[var(--color-surface-2)]/60 transition-colors">
                      <td className="px-5 py-3 font-medium text-[var(--color-text)]">{row.name}</td>
                      <td className="px-5 py-3 text-center">
                        {row.a ? <CheckCircle2 size={16} className="text-[var(--color-good)] mx-auto" /> : <XCircle size={16} className="text-[var(--color-text-faint)] mx-auto" />}
                      </td>
                      <td className="px-5 py-3 text-center">
                        {row.co ? <CheckCircle2 size={16} className="text-[var(--color-good)] mx-auto" /> : <XCircle size={16} className="text-[var(--color-text-faint)] mx-auto" />}
                      </td>
                      <td className="px-5 py-3 text-center">
                        {row.e ? <CheckCircle2 size={16} className="text-[var(--color-good)] mx-auto" /> : <XCircle size={16} className="text-[var(--color-text-faint)] mx-auto" />}
                      </td>
                      <td className="px-5 py-3 text-center">
                        {row.v ? <CheckCircle2 size={16} className="text-[var(--color-good)] mx-auto" /> : <XCircle size={16} className="text-[var(--color-text-faint)] mx-auto" />}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>

      {/* Create User Modal */}
      <Modal
        isOpen={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        title="Invite New User"
        subtitle="Add a new member to PolicyMesh and grant an appropriate role."
      >
        <form onSubmit={handleCreateUser} className="space-y-4">
          {createError && (
            <div className="text-xs text-[var(--color-bad)] bg-[var(--color-bad-light)] border border-[var(--color-bad)]/30 rounded-lg p-2.5 flex items-center gap-2">
              <AlertTriangle size={14} className="shrink-0" />
              <span>{createError}</span>
            </div>
          )}

          <div>
            <label className="block text-xs font-medium text-[var(--color-text-dim)] mb-1">
              Email Address *
            </label>
            <input
              type="email"
              value={createForm.email}
              onChange={(e) => setCreateForm((f) => ({ ...f, email: e.target.value }))}
              placeholder="engineer@company.com"
              className="field-input text-xs"
              required
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-[var(--color-text-dim)] mb-1">
              Initial Password * <span className="text-[var(--color-text-faint)]">(Min 8 characters)</span>
            </label>
            <input
              type="password"
              value={createForm.password}
              onChange={(e) => setCreateForm((f) => ({ ...f, password: e.target.value }))}
              placeholder="••••••••••••"
              className="field-input text-xs"
              required
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-[var(--color-text-dim)] mb-1">
              Role Assignment *
            </label>
            <select
              value={createForm.role}
              onChange={(e) => setCreateForm((f) => ({ ...f, role: e.target.value }))}
              className="field-input text-xs"
            >
              {ROLES.map((r) => (
                <option key={r} value={r}>
                  {r}
                </option>
              ))}
            </select>
          </div>

          <div className="flex items-center justify-end gap-2 pt-3 border-t border-[var(--color-border)]">
            <Button variant="ghost" size="md" onClick={() => setShowCreateModal(false)}>
              Cancel
            </Button>
            <Button variant="primary" size="md" type="submit" loading={createSubmitting}>
              Create User
            </Button>
          </div>
        </form>
      </Modal>

      {/* Edit User Modal */}
      <Modal
        isOpen={!!editingUser}
        onClose={() => setEditingUser(null)}
        title="Edit User Role"
        subtitle={`Update role assignment for ${editingUser?.email}`}
      >
        <form onSubmit={handleEditUser} className="space-y-4">
          {editError && (
            <div className="text-xs text-[var(--color-bad)] bg-[var(--color-bad-light)] border border-[var(--color-bad)]/30 rounded-lg p-2.5 flex items-center gap-2">
              <AlertTriangle size={14} className="shrink-0" />
              <span>{editError}</span>
            </div>
          )}

          <div>
            <label className="block text-xs font-medium text-[var(--color-text-dim)] mb-1">
              Role Assignment *
            </label>
            <select
              value={editForm.role}
              onChange={(e) => setEditForm((f) => ({ ...f, role: e.target.value }))}
              className="field-input text-xs"
            >
              {ROLES.map((r) => (
                <option key={r} value={r}>
                  {r}
                </option>
              ))}
            </select>
          </div>

          <div className="flex items-center justify-end gap-2 pt-3 border-t border-[var(--color-border)]">
            <Button variant="ghost" size="md" onClick={() => setEditingUser(null)}>
              Cancel
            </Button>
            <Button variant="primary" size="md" type="submit" loading={editSubmitting}>
              Update Role
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete User Confirmation */}
      <Modal
        isOpen={!!deletingUser}
        onClose={() => setDeletingUser(null)}
        title="Confirm User Deletion"
        subtitle={`Are you sure you want to revoke access for ${deletingUser?.email}?`}
      >
        <div className="flex items-center justify-end gap-2 pt-4">
          <Button variant="ghost" size="md" onClick={() => setDeletingUser(null)}>
            Cancel
          </Button>
          <Button variant="danger" size="md" onClick={handleDeleteUser} loading={deleteSubmitting}>
            Delete User
          </Button>
        </div>
      </Modal>
    </div>
  );
}
