import { CloudArrowDownIcon } from '@heroicons/react/24/outline'
import {download as snapshotDesktopMacDownload} from './downloads/download-snapshot-desktop-mac';
import {download as snapshotServerDownload} from './downloads/download-snapshot-server';
import {download as snapshotContainerDownload} from './downloads/download-snapshot-container';
// import {download as snapshotDisabledDownload} from './downloads/download-snapshot-disabled';

const snapshotDownloads = [
    snapshotServerDownload,
    snapshotContainerDownload,
    snapshotDesktopMacDownload
]

export default function pageDownloads() {

    return (
        <div className="bg-white py-4 sm:py-8" id="downloads">
            <div className="mx-auto max-w-7xl px-6 lg:px-8">
                <div className="mx-auto max-w-2xl lg:text-center">
                    <h2 className="text-base font-semibold leading-7 text-indigo-600">Downloads</h2>
                    <p className="mt-2 text-3xl font-bold tracking-tight text-gray-900 sm:text-4xl">
                        Snapshots
                    </p>
                    <p className="mt-6 text-lg leading-8 text-gray-600">
                        Snapshots of the latest releases are available for download. The snapshots are built from the latest code and are not tested.
                    </p>
                </div>
                <div className="mx-auto mt-16 max-w-2xl sm:mt-20 lg:mt-24 lg:max-w-4xl">
                    <dl className="grid max-w-xl grid-cols-1 gap-x-8 gap-y-10 lg:max-w-none lg:grid-cols-2 lg:gap-y-16">
                        {snapshotDownloads.map((download) => download.enabled ? (
                            <div key="snapshot_mac" className="relative pl-16">
                                <dt className="text-base font-semibold leading-7 text-gray-900 underline">
                                    <a href={download.href}>
                                        <div
                                            className="absolute left-0 top-0 flex h-10 w-10 items-center justify-center rounded-lg bg-indigo-600">
                                            <CloudArrowDownIcon className="h-6 w-6 text-white" aria-hidden="true"/>
                                        </div>
                                        {download.title}
                                    </a>
                                </dt>
                                <dd className="mt-2 text-base leading-7 text-gray-600">{download.description}</dd>
                                <dd className="mt-2 text-base leading-7 text-gray-400">Updated {download.created}</dd>
                            </div>
                        ) : (null) ) }
                    </dl>
                </div>
            </div>
        </div>
    );
}