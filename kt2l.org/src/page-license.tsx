export default function PageLicense() {

    return (
        <div className="bg-white py-4 sm:py-8" id="downloads">
        <div dangerouslySetInnerHTML={{ __html: "<a name='license'></a>" }} />
        <div className="mx-auto max-w-7xl px-6 lg:px-8">
                <div className="mx-auto max-w-4xl lg:text-center">
                    <h2 className="text-base font-semibold leading-7 text-indigo-600">License</h2>
                    <p className="mt-6 text-lg leading-8 text-gray-600">
                        The software is licensed under the GPL 3.0 license. The software is free to use and modify.
                        The software is provided as is without any warranty. <a href="https://www.gnu.org/licenses/gpl-3.0.en.html">See also GPLv3.</a>
                    </p>
                </div>
            </div>
        </div>
    );
}