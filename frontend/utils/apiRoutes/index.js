import { AppSetting } from "@configuration/AppSetting";
import * as authRoutes from "./authRoutes";
import * as chatRoutes from "./chatRoutes";
import * as policiesRoutes from "./policiesRoutes";
import * as actionsRoutes from "./actionsRoutes";

export const ApiRootUrl = `${AppSetting.ApiServerURL}/`;

export const routesMapping = {
  auth: authRoutes,
  chat: chatRoutes,
  policies: policiesRoutes,
  actions: actionsRoutes,
};

export const Route = (route, defaultBase = ApiRootUrl) => {
  const path = String(route).replace(/^\//, "");
  return `${defaultBase}${path}`;
};
