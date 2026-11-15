package com.example.demo.catalyst;

import com.catalyst.config.ZCThreadLocal;
import com.zc.api.APIConstants.ZCUserScope;
import com.zc.common.ZCProject;

public final class CatalystContext {

    private static final ThreadLocal<ZCProject> ADMIN = new ThreadLocal<ZCProject>();
    private static final ThreadLocal<ZCProject> USER = new ThreadLocal<ZCProject>();

    private CatalystContext() {
        // only static methods; no objects of this class
    }

    /** Called by CatalystRequestFilter at the start of a request (after CatalystSDK.init). */
    static void open() {
        // Server rights: this always works when the request came through Catalyst.
        ADMIN.set(ZCProject.initProject("sample_admin", ZCUserScope.ADMIN));

        // The signed-in person: fails when the request carried no (or an expired) sign-in.
        try {
            USER.set(ZCProject.initProject("sample_user", ZCUserScope.USER));
        } catch (RuntimeException notSignedIn) {
            USER.remove();
        }
    }

    /** Called by CatalystRequestFilter at the end of every request. */
    static void close() {
        ADMIN.remove();
        USER.remove();
        ZCThreadLocal.clearAllKeys(); // the SDK's own per-thread values
    }

    /** For Data Store calls (repository layer). */
    public static ZCProject admin() {
        ZCProject project = ADMIN.get();
        if (project == null) {
            throw new IllegalStateException("Catalyst is not set up for this request");
        }
        return project;
    }

    /** For "who is signed in?" (null when nobody is). */
    public static ZCProject user() {
        return USER.get();
    }
}