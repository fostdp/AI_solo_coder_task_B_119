const VirtualWeighingComponent = require('./VirtualWeighingComponent');

function createVirtualWeighing(containerId, api, options) {
    return new VirtualWeighingComponent(containerId, api, options);
}

function initVirtualWeighing(containerId, api, options) {
    const component = new VirtualWeighingComponent(containerId, api, options);
    component.init();
    return component;
}

module.exports = {
    VirtualWeighingComponent,
    createVirtualWeighing,
    initVirtualWeighing,
    default: VirtualWeighingComponent
};

if (typeof window !== 'undefined') {
    window.VirtualWeighing = {
        VirtualWeighingComponent,
        createVirtualWeighing,
        initVirtualWeighing
    };
}
