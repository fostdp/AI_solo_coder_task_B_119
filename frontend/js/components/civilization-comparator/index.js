import CivilizationComparatorComponent from './CivilizationComparatorComponent.js';

const createCivilizationComparator = (containerId, api, options = {}) => {
    const component = new CivilizationComparatorComponent(containerId, api, options);
    return component;
};

if (typeof window !== 'undefined') {
    window.CivilizationComparatorComponent = CivilizationComparatorComponent;
    window.createCivilizationComparator = createCivilizationComparator;
}

export { CivilizationComparatorComponent, createCivilizationComparator };
export default CivilizationComparatorComponent;
