//import React from 'react';
//import logo from './kt2l-logo.svg';
import './App.css';
//import { useState } from 'react'
//import { Dialog } from '@headlessui/react'
//import { Bars3Icon, XMarkIcon } from '@heroicons/react/24/outline'
import pageHeader from './page-header'
import pageGeneralInformation from './page-general-information'
import pageDownloads from './page-downloads'
import pageLicense from './page-license'

function App() {
  return (
      <div className="bg-white">
          {pageHeader()}
          {pageGeneralInformation()}
          {pageDownloads()}
          {pageLicense()}
      </div>
  );
}

export default App;
