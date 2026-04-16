import { CloudArrowDownIcon } from '@heroicons/react/24/outline'

const downloads = [
    {
        title: 'Desktop Mac OSX (Silicon) Bundle',
        description: 'DMG installer for Apple Silicon Macs. Java JDK 21 is included.',
        help: '/docs/installation/desktop#mac-osx-bundle',
    },
    {
        title: 'Desktop Linux (amd64) Bundle',
        description: 'DEB package for Debian/Ubuntu. Java JDK 21 is included.',
        help: '/docs/installation/desktop#linux-bundle',
    },
    {
        title: 'Server ZIP',
        description: 'Can be started as server and accessed via browser. Java JDK 21 is required.',
        help: '/docs/installation/server',
    },
    {
        title: 'Docker Container',
        description: 'Available on Docker Hub as mhus/kt2l-server.',
        help: '/docs/installation/container',
    },
];

export default function PageDownloads() {

    return (
        <div className="bg-white py-4 sm:py-8" id="downloads">
            <div dangerouslySetInnerHTML={{ __html: "<a name='downloads'></a>" }} />
            <div className="mx-auto max-w-7xl px-6 lg:px-8">
                <div className="mx-auto max-w-4xl lg:text-center">
                    <h2 className="text-base font-semibold leading-7 text-indigo-600">Downloads</h2>
                    <p className="mt-6 text-lg leading-8 text-gray-600">
                        Download the latest releases from GitHub. Snapshots and stable releases are available
                        for multiple platforms.
                    </p>
                    <p className="mt-6">
                        <a href="https://github.com/mhus/kt2l/releases"
                           target="_blank" rel="noreferrer"
                           className="rounded-md bg-indigo-600 px-6 py-3 text-lg font-semibold text-white shadow-sm hover:bg-indigo-500">
                            View Releases on GitHub
                        </a>
                    </p>
                </div>
                <div className="mx-auto mt-16 max-w-4xl sm:mt-20 lg:mt-24 lg:max-w-4xl">
                    <dl className="grid max-w-xl grid-cols-1 gap-x-8 gap-y-10 lg:max-w-none lg:grid-cols-2 lg:gap-y-16">
                        {downloads.map((download) => (
                            <div key={download.title} className="border rounded-lg bg-slate-100 relative pl-16">
                                <dt className="text-base font-semibold leading-7 text-gray-900">
                                    <div className="absolute left-0 top-0 flex h-10 w-10 items-center justify-center rounded-lg bg-indigo-600">
                                        <CloudArrowDownIcon className="h-6 w-6 text-white" aria-hidden="true"/>
                                    </div>
                                    {download.title}
                                </dt>
                                <dd className="mt-2 text-base leading-7 text-gray-600">{download.description}</dd>
                                <dd className="mt-2 text-base leading-7">
                                    <a href={download.help} className="text-indigo-600 hover:text-indigo-500">
                                        Installation guide
                                    </a>
                                </dd>
                            </div>
                        ))}
                    </dl>
                </div>
            </div>
        </div>
    );
}
