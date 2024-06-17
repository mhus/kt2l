import logo from './kt2l-logo.svg';

export default function PageGeneralInformation() {

    return (
        <div className="relative isolate px-6 pt-14 lg:px-8">
            <div
                aria-hidden="true"
            >
                <div
                    style={{
                        clipPath:
                            'polygon(74.1% 44.1%, 100% 61.6%, 97.5% 26.9%, 85.5% 0.1%, 80.7% 2%, 72.5% 32.5%, 60.2% 62.4%, 52.4% 68.1%, 47.5% 58.3%, 45.2% 34.5%, 27.5% 76.7%, 0.1% 64.9%, 17.9% 100%, 27.6% 76.8%, 76.1% 97.7%, 74.1% 44.1%)',
                    }}
                />
            </div>
            <div className="mx-auto max-w-2xl py-32 sm:py-8">
                <div className="text-center">
                    <h1 className="text-4xl font-bold tracking-tight text-gray-900 sm:text-6xl">
                        KT2L
                    </h1>
                    <center>
                        <img src={logo} className="App-logo" alt="logo" width={300}/>
                    </center>
                    <p className="mt-6 text-lg leading-8 text-gray-600">
                        KT2L (ktool) is a web based tool to manage your kubernetes clusters. The tool can be
                        installed locally or
                        in the cluster. The goal is to provide a control center and do things smarter. It adresses
                        administrators
                        and developers.
                    </p>
                </div>
            </div>
        </div>
    );
}