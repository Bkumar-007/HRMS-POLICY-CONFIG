import { AppSetting } from "@configuration/AppSetting";
import * as authRoutes from "./authRoutes";
import * as chatRoutes from "./chatRoutes";
import * as policiesRoutes from "./policiesRoutes";
import * as actionsRoutes from "./actionsRoutes";
import * as peopleRoutes from "./peopleRoutes";
import * as breachesRoutes from "./breachesRoutes";
import * as helpdeskRoutes from "./helpdeskRoutes";
import * as leaveRoutes from "./leaveRoutes";

export const ApiRootUrl = `${AppSetting.ApiServerURL}/`;

export const routesMapping = {
  auth: authRoutes,
  chat: chatRoutes,
  policies: policiesRoutes,
  actions: actionsRoutes,
  people: peopleRoutes,
  breaches: breachesRoutes,
  helpdesk: helpdeskRoutes,
  leave: leaveRoutes,
};

export const Route = (route, defaultBase = ApiRootUrl) => {
  const path = String(route).replace(/^\//, "");
  return `${defaultBase}${path}`;
};
