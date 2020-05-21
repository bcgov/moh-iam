import { shallowMount } from '@vue/test-utils';
import TheFooter from '@/components/template/TheFooter.vue';

describe('BCGovFooter.vue', () => {
  it('renders', () => {
    const wrapper = shallowMount(TheFooter);
    expect(wrapper.text()).toMatch('Contact Us');
  });
});