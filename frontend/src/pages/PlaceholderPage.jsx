import Topbar from "../components/layout/Topbar";

export default function PlaceholderPage({ title, subtitle }) {
  return (
    <div>
      <Topbar title={title} subtitle={subtitle} />
      <div className="px-6 lg:px-8 mt-4">
        <div className="card p-10 flex flex-col items-center justify-center text-center">
          <p className="text-white font-medium">This section is wired up and ready to build on.</p>
          <p className="text-sm text-[var(--color-text-faint)] mt-1 max-w-md">
            The API client for this area already exists in <code className="text-[var(--color-text-dim)]">src/api/</code>.
            See <code className="text-[var(--color-text-dim)]">design.md</code> for the endpoints this page should call.
          </p>
        </div>
      </div>
    </div>
  );
}
