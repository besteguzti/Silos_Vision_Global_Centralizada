import { useEffect, useState } from "react";

import MainPage from "./Pages/MainPage";
import ArubaPage from "./Pages/ArubaPage";
import CitrixPage from "./Pages/CitrixPage";
import Microsoft365Page from "./Pages/Microsoft365Page";
import GlpiPage from "./Pages/GlpiPage";
import AnalysisPage from "./Pages/AnalysisPage";
import TestScenarioPage from "./Pages/TestScenarioPage";
import ThresholdConfigurationPage from "./Pages/ThresholdConfigurationPage";

const pageRoutes = {
    main: "/",
    aruba: "/aruba",
    citrix: "/citrix",
    m365: "/microsoft365",
    glpi: "/glpi",
    analysis: "/análisis",
    test: "/test",
    config: "/configuración"
};

function App() {

    const [activePage, setActivePage] =
        useState(resolveInitialPage);

    useEffect(() => {

        const handlePopState = () => {

            setActivePage(resolveInitialPage());
        };

        window.addEventListener("popstate", handlePopState);

        return () => window.removeEventListener("popstate", handlePopState);

    }, []);

    const navigate = (page) => {

        setActivePage(page);
        window.history.pushState({}, "", pageRoutes[page]);
    };

    const renderPage = () => {

        switch (activePage) {

            case "aruba":
                return <ArubaPage />;

            case "citrix":
                return <CitrixPage />;

            case "m365":
                return <Microsoft365Page />;

            case "glpi":
                return <GlpiPage />;

            case "analysis":
                return <AnalysisPage />;

            case "test":
                return <TestScenarioPage />;

            case "config":
                return <ThresholdConfigurationPage />;

            default:
                return <MainPage />;
        }
    };

    return (
        <div>
            <nav className="app-nav">
                <button
                    className={activePage === "main" ? "active" : ""}
                    onClick={() => navigate("main")}
                >
                    Principal
                </button>

                <button
                    className={activePage === "aruba" ? "active" : ""}
                    onClick={() => navigate("aruba")}
                >
                    Aruba
                </button>

                <button
                    className={activePage === "citrix" ? "active" : ""}
                    onClick={() => navigate("citrix")}
                >
                    Citrix
                </button>

                <button
                    className={activePage === "m365" ? "active" : ""}
                    onClick={() => navigate("m365")}
                >
                    Microsoft 365
                </button>

                <button
                    className={activePage === "glpi" ? "active" : ""}
                    onClick={() => navigate("glpi")}
                >
                    GLPI
                </button>

                <button
                    className={activePage === "analysis" ? "active" : ""}
                    onClick={() => navigate("analysis")}
                >
                    Análisis
                </button>

                <button
                    className={activePage === "test" ? "active" : ""}
                    onClick={() => navigate("test")}
                >
                    Banco de pruebas
                </button>

                <button
                    className={activePage === "config" ? "active" : ""}
                    onClick={() => navigate("config")}
                >
                    Configuración
                </button>
            </nav>

            {renderPage()}
        </div>
    );
}

function resolveInitialPage() {
    const pathname = window.location.pathname;

    if (pathname === "/aruba") {
        return "aruba";
    }

    if (pathname === "/citrix") {
        return "citrix";
    }

    if (pathname === "/microsoft365") {
        return "m365";
    }

    if (pathname === "/glpi") {
        return "glpi";
    }

    if (pathname === "/análisis" || pathname === "/analysis") {
        return "analysis";
    }

    if (pathname === "/test") {
        return "test";
    }

    if (pathname === "/configuración" || pathname === "/configuration") {
        return "config";
    }

    return "main";
}

export default App;

