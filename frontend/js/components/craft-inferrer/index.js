import CraftInferrerComponent from './CraftInferrerComponent.js';

export { CraftInferrerComponent };
export default CraftInferrerComponent;

if (typeof window !== 'undefined') {
    window.CraftInferrer = {
        CraftInferrerComponent,
        create: (containerId, api, options) => {
            const component = new CraftInferrerComponent(containerId, api, options);
            component.init();
            return component;
        }
    };
}
