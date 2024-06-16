import { CloudArrowDownIcon, CloudIcon, InformationCircleIcon } from '@heroicons/react/24/outline'
import {downloadList as snapshotDownloads} from './downloads/download-snapshot-list';

export default function pageDownloads() {

    return (
        <div className="bg-white py-4 sm:py-8" id="downloads">
            <div dangerouslySetInnerHTML={{ __html: "<a name='downloads'></a>" }} />
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
                            <div className={"relative pl-16 " + (download.title === "Desktop Windows (amd64) Installer" ? 'line-through' : '')}>
                                {download.href.length !== 0 ? (
                                    <dt className="text-base font-semibold leading-7 text-gray-900 underline">
                                        <a href={download.href}>
                                            <div
                                                className="absolute left-0 top-0 flex h-10 w-10 items-center justify-center rounded-lg bg-indigo-600">
                                                <CloudArrowDownIcon className="h-6 w-6 text-white" aria-hidden="true"/>
                                            </div>
                                            {download.title}
                                        {download.href_help.length !== 0 ? (
                                            <a href={download.href_help}>
                                                <div
                                                    className="absolute right-0 top-0 flex h-10 w-10 items-center justify-center">
                                                    <InformationCircleIcon className="h-6 w-6 text-indigo-600" aria-hidden="true"/>
                                                </div>
                                            </a>
                                        ) : (null) }
                                        </a>
                                    </dt>
                                ) : (
                                    <dt className="text-base font-semibold leading-7 text-gray-900">
                                        <div
                                            className="absolute left-0 top-0 flex h-10 w-10 items-center justify-center rounded-lg bg-indigo-600">
                                            <CloudIcon className="h-6 w-6 text-white" aria-hidden="true"/>
                                        </div>
                                        {download.title}
                                        {download.href_help.length !== 0 ? (
                                            <a href={download.href_help}>
                                                <div
                                                    className="absolute right-0 top-0 flex h-10 w-10 items-center justify-center">
                                                    <InformationCircleIcon className="h-6 w-6 text-indigo-600" aria-hidden="true"/>
                                                </div>
                                            </a>
                                        ) : (null) }
                                    </dt>
                                        ) }
                                <dd className="mt-2 text-base leading-7 text-gray-600">{download.description}</dd>
                                <dd className="mt-2 text-base leading-7 text-gray-400">{download.size.length !== 0 ? download.size + ", " : ""}Updated {download.created}</dd>
                            </div>
                        ) : (null) ) }
                    </dl>
                </div>
            </div>
        </div>
    );
}