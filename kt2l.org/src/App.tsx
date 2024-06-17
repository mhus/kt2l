import './App.css';
import PageHeader from './page-header'
import PageGeneralInformation from './page-general-information'
import PageDownloads from './page-downloads'
import PageLicense from './page-license'
import PageDisclaimer from './page-disclaimer';

function App() {
  return (
      <div className="bg-white">
          {PageHeader()}
          {PageGeneralInformation()}
          {PageDisclaimer()}
          {PageDownloads()}
          {PageLicense()}
      </div>
  );
}

export default App;
