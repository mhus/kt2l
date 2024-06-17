export default function PageDisclaimer() {

    return (
        <div className="bg-white py-4 sm:py-8" id="downloads">
        <div dangerouslySetInnerHTML={{ __html: "<a name='disclaimer'></a>" }} />
        <div className="mx-auto max-w-7xl px-6 lg:px-8">
                <div className="mx-auto max-w-2xl lg:text-center">
                    <h2 className="text-base font-semibold leading-7 text-indigo-600">Disclaimer</h2>
                    <p className="mt-6 text-lg leading-8 text-gray-600">
                        Disclaimer: This software is under heavy development and updated frequently. Bugs and issues are expected.
                        This software is open source your use of the software on this site means you understand they are not supported or guaranteed in any way.
                    </p>
                </div>
            </div>
        </div>
    );
}