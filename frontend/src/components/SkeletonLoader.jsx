const SkeletonLoader = ({ type = "card", count = 1 }) => {
  if (type === "card") {
    return (
      <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
        {Array.from({ length: count }).map((_, i) => (
          <div key={i} className="bg-bg-card border border-border rounded-xl p-4 animate-pulse">
            <div className="h-40 bg-bg-elevated rounded-lg mb-3" />
            <div className="h-5 bg-bg-elevated rounded w-3/4 mx-auto mb-3" />
            <div className="h-4 bg-bg-elevated rounded w-1/2 mx-auto" />
          </div>
        ))}
      </div>
    );
  }

  if (type === "detail") {
    return (
      <div className="animate-pulse space-y-6 p-4 md:p-8">
        <div className="flex gap-8">
          <div className="w-40 h-60 bg-bg-elevated rounded-xl" />
          <div className="flex-1 space-y-4">
            <div className="h-8 bg-bg-elevated rounded w-1/3" />
            <div className="h-4 bg-bg-elevated rounded w-1/2" />
            <div className="grid grid-cols-3 gap-4 mt-6">
              {Array.from({ length: 6 }).map((_, i) => (
                <div key={i} className="h-24 bg-bg-elevated rounded-lg" />
              ))}
            </div>
          </div>
        </div>
        <div className="flex gap-2">
          {Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="h-10 w-24 bg-bg-elevated rounded-lg" />
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="flex justify-center items-center h-64">
      <div className="flex items-center gap-3 text-text-secondary">
        <div className="w-5 h-5 border-2 border-accent-cyan border-t-transparent rounded-full animate-spin" />
        Loading...
      </div>
    </div>
  );
};

export default SkeletonLoader;
