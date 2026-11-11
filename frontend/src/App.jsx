import { lazy, Suspense } from 'react';
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import ErrorBoundary from './components/ErrorBoundary'
import Layout from './components/Layout'

const Catalog = lazy(() => import('./pages/Catalog'));
const Roster = lazy(() => import('./pages/Roster'));
const TeamBuilder = lazy(() => import('./pages/TeamBuilder'));
const Calculator = lazy(() => import('./pages/Calculator'));
const SharedTeam = lazy(() => import('./pages/SharedTeam'));

const LoadingFallback = () => (
  <div className="flex items-center justify-center h-64">
    <div className="text-text-primary text-xl">Loading...</div>
  </div>
);

const App = () => {
  return (
    <BrowserRouter>
      <ErrorBoundary>
        <Layout>
          <Suspense fallback={<LoadingFallback />}>
            <Routes>
              <Route path='/' element={<Catalog />}/>
              <Route path='/roster' element={<Roster />}/>
              <Route path='/builder' element={<TeamBuilder />}/>
              <Route path='/spd' element={<Calculator />}/>
              {/* Share links: /t/aZ3kP9qX */}
              <Route path='/t/:shareCode' element={<SharedTeam />}/>
            </Routes>
          </Suspense>
        </Layout>
      </ErrorBoundary>
    </BrowserRouter>
  )
}

export default App
