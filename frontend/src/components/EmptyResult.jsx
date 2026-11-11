const EmptyResult = ({ onClear }) => (
  <div className="py-16 text-center">
    <p className="font-hand text-3xl text-text-secondary">nobody matches those filters</p>
    <button type="button" onClick={onClear} className="mt-2 text-sm text-accent-cyan hover:text-accent-cyan/80">
      Clear all filters
    </button>
  </div>
);

export default EmptyResult;
