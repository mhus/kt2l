export default function PageDem() {

    return (
        <div className="bg-white py-4 sm:py-8" id="demo">
        <div dangerouslySetInnerHTML={{ __html: "<a name='demo'></a>" }} />
        <div className="mx-auto max-w-7xl px-6 lg:px-8">
                <div className="mx-auto max-w-2xl lg:text-center">
                    <h2 className="text-base font-semibold leading-7 text-indigo-600">Demo</h2>
                    <p className="mt-6 text-lg leading-8 text-gray-600">
                    You can access the demo of the software by clicking the link below. The demo is a live version
                    of the software and is not guaranteed to be up at all times. You can login with different users
                    and roles to see the different flavors of the software.
                    </p>
                    <p className="mt-6 text-lg leading-8 text-gray-600 underline">
                        <a href="http://demo.kt2l.org:9080" target="_blank" rel="noreferrer">Open Demo</a>
                    </p>
                    <p className="mt-6 text-lg leading-8 text-gray-600 lg:text-center">
                        <div className="not-prose relative bg-slate-50 rounded-xl overflow-hidden dark:bg-slate-800/25 lg:text-left indent-4" >
                            <b>Login:</b>
                            <table className="table-auto indent-8">
                                <thead>
                                    <tr>
                                        <th>Username</th><th>Password</th><th>Role</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr>
                                        <td>admin</td><td>jKjau86G</td><td>administrative user for the cluster</td>
                                    </tr>
                                    <tr>
                                        <td>user</td><td>hIUYHh72jhb</td><td>user or developer for the cluster</td>
                                    </tr>
                                    <tr>
                                        <td>viewer</td><td>uiJKjb127khj</td><td>user or developer for the cluster</td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                    </p>
                    <p className="mt-6 text-lg leading-8 text-gray-600">
                    The kubernetes cluster is running on a single node and access is limited to read only.
                    But you are able to scale deployments.
                    Even if you logn as an admin you will not be able to make changes to the cluster. The cluster
                    will be reset every 24 hours.
                    The underlying server is not very powerful so please be patient with the response times.
                    A Ollama server is running to provide the AI features.
                    </p>
                </div>
            </div>
        </div>
    );
}