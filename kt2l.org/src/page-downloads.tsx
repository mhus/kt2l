import { CloudArrowDownIcon } from '@heroicons/react/24/outline'

export default function pageDownloads() {

    return (
        <div className="bg-white py-4 sm:py-8">
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
                        <div key="snapshot_mac" className="relative pl-16">
                            <dt className="text-base font-semibold leading-7 text-gray-900 underline">
                                <a href="snapshots/kt2l-snapshot.dmg">
                                    <div
                                        className="absolute left-0 top-0 flex h-10 w-10 items-center justify-center rounded-lg bg-indigo-600">
                                        <CloudArrowDownIcon className="h-6 w-6 text-white" aria-hidden="true"/>
                                    </div>
                                    Mac OS X Bundled
                                </a>
                            </dt>
                            <dd className="mt-2 text-base leading-7 text-gray-600">Can be executed directly in Mac OS X M1. Java JDK 21 is included.</dd>
                        </div>
                        <div key="snapshot_server" className="relative pl-16">
                            <dt className="text-base font-semibold leading-7 text-gray-900 underline">
                                <a href="snapshots/kt2l-snapshot.zip">
                                    <div
                                        className="absolute left-0 top-0 flex h-10 w-10 items-center justify-center rounded-lg bg-indigo-600">
                                        <CloudArrowDownIcon className="h-6 w-6 text-white" aria-hidden="true"/>
                                    </div>
                                    Server Bundled
                                </a>
                            </dt>
                            <dd className="mt-2 text-base leading-7 text-gray-600">Can be started as Server and accessed via Browser. Also locally. Java JDK 21 is required.</dd>
                        </div>
                    </dl>
                </div>
            </div>
        </div>
    );
}