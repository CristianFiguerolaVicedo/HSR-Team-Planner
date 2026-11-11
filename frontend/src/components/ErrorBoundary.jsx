import { Component } from "react";

export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="flex items-center justify-center h-64">
          <div className="text-center">
            <div className="text-accent-red text-xl font-bold mb-2">Something went wrong</div>
            <div className="text-text-secondary text-sm">{this.state.error?.message}</div>
            <button
              onClick={() => window.location.reload()}
              className="mt-4 px-4 py-2 bg-accent-red/20 text-accent-red rounded-lg hover:bg-accent-red/30"
            >
              Reload page
            </button>
          </div>
        </div>
      );
    }
    return this.props.children;
  }
}
