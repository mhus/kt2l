import './App.css';
import PageHeader from './page-header'
import PageGeneralInformation from './page-general-information'
import PageDownloads from './page-downloads'
import PageDemo from './page-demo'
import PageLicense from './page-license'
import PageDisclaimer from './page-disclaimer';

function App() {
  return (
      <div className="bg-white">
          {PageHeader()}
          {PageGeneralInformation()}
          {PageDisclaimer()}
          {PageDownloads()}
          {PageDemo()}
          {PageLicense()}
      </div>
  );
}

export default App;
